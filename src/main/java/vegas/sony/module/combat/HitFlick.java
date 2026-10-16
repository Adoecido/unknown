package vegas.sony.module.combat;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.event.impl.EventKeyInputBase;
import vegas.sony.event.impl.EventKeyPress;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.event.impl.EventSetSprinting;
import vegas.sony.event.impl.SyntheticAttackRequestEvent;
import vegas.sony.input.AttackKeyController;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.module.combat.hitflick.HitFlickAdaptiveRotationController;
import vegas.sony.module.combat.silentaura.SilentAuraAdaptiveRotationEntry;
import vegas.sony.module.combat.silentaura.SilentAuraRotationMode;
import vegas.sony.module.control.PrimaryActionControlClaim;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.rotation.AdaptiveRotationController;
import vegas.sony.rotation.RotationAngles;
import vegas.sony.rotation.RotationControlClaim;
import vegas.sony.rotation.RotationManager;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.utils.MathUtil;
import vegas.sony.utils.MutableColor;
import vegas.sony.utils.RotationUtil;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.Vec3d;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.utils.render.RenderUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.EntityTargetFilterValue;
import vegas.sony.value.LimitValue;
import vegas.sony.value.NumberValue;
import vegas.sony.mapping.mappings.MEntity;
import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.BlockPos;
import vegas.sony.wrapper.impl.BlockState;
import vegas.sony.wrapper.impl.Blocks;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.RenderManager;
import vegas.sony.wrapper.impl.UseEntityPacketBridge;
import vegas.sony.wrapper.impl.Vec3;
import vegas.sony.wrapper.impl.World;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Color;
import org.lwjgl.opengl.GL11;

public class HitFlick
extends Mod {
    private final BooleanValue randomizeOffset;
    private final NumberValue chance;
    private final NumberValue angle;
    private static final int FLICK_AWAY_INDICATOR_COLOR = 0xC95DFF;
    private static final int FLICK_BACK_INDICATOR_COLOR = 0x55FF77;
    private final BooleanValue webPriority;
    private final NumberValue webRange;
    private final NumberValue webFov;
    private boolean attackQueued;
    private boolean cancelSprint;
    private boolean attackKeyHeld;
    private int stateTicks;
    private final Queue<EventPacketSend> heldPackets;
    private final NumberValue flickDelay;
    private final PacketDispatchGuard dispatchGuard;
    private boolean blinking;
    private final EntityTargetFilterValue targetFilter = EntityTargetFilterValue.createForModule(this);
    private SilentAuraRotationMode rotationMode;
    private final RotationControlClaim rotationClaim;
    private final PrimaryActionControlClaim primaryClaim;
    private int blinkTicks;
    private boolean forwardKeyForced;
    private AdaptiveRotationController rotationController;
    private final TimerUtil flickDelayTimer;
    private float flickAngle;
    private SilentAura silentAura;
    private final BooleanValue selectHits;
    private boolean sprintCancelPending;
    private EntityLivingBase currentTarget;
    private boolean backKeyForced;
    private boolean attacked;
    private final BooleanValue strafeInvert;
    private final NumberValue randomizeOffsetRange;
    private boolean claimHeld;
    private final BooleanValue blink = BooleanValue.create(this, "Blink //$ui", false, "Chokes outgoing packets during the flick and flushes once the attack is sent //$ui");
    private final LimitValue allowedItems;
    private final BooleanValue limitToItems;
    private final BooleanValue showDirection = BooleanValue.create(this,
            "Show direction //$ui", true,
            "Shows a 3D arrow beside your player for the selected knockback direction //$ui");
    private final NumberValue directionDuration = NumberValue.create(this,
            "Direction duration //$ui", "#", "ms", 100.0, 450.0, 1500.0, 25.0,
            "How long the 3D direction arrow remains visible after a flick starts //$ui");
    private final ColorValue directionColor = ColorValue.create(this,
            "Direction color //$ui", new Color(201, 93, 255));
    private long directionIndicatorUntil;
    private float directionIndicatorYaw;

    private double[] getTargetAimPoint(EntityLivingBase target) {
        EntityPlayerSP player = Minecraft.thePlayer();
        Vec3d closestPoint = RotationUtil.T(player, target.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl(), 0.0, 0.0, 0.0);
        return new double[]{closestPoint.getX(), closestPoint.getY(), closestPoint.getZ()};
    }

    private boolean isValidTarget(EntityLivingBase target) {
        if (target.isNull()) {
            return false;
        }
        if (target.equals(Minecraft.thePlayer())) {
            return false;
        }
        if (target.w$src$F$15l9epb() <= 0.0f || target.M$src$Z$ff28xj()) {
            return false;
        }
        if (!this.targetFilter.isValidTarget(target)) {
            return false;
        }
        double distance = Minecraft.thePlayer().i(target.z(), target.N(), target.h());
        return distance <= 5.0;
    }

    private boolean startFlick(EntityLivingBase target) {
        if (this.rotationMode != SilentAuraRotationMode.IDLE || !this.canStartFlick()) {
            return false;
        }
        if (!this.isValidTarget(target)) {
            return false;
        }
        if (this.selectHits.getEffectiveValue().booleanValue() && !this.isTargetVulnerable(target)) {
            return false;
        }
        if (!this.passesChanceRoll()) {
            return false;
        }
        if (!this.rotationClaim.isOwnedBy(this) && !this.rotationClaim.acquire(this, true)) {
            return false;
        }
        this.currentTarget = target;
        this.attackQueued = true;
        this.flickAngle = this.resolveStrafeAdjustedAngle();
        this.showDirectionIndicator(target);
        this.rotationMode = SilentAuraRotationMode.FLICKING_AWAY;
        this.stateTicks = 0;
        this.cancelSprint = true;
        this.flickDelayTimer.reset();
        this.startBlink();
        this.updateMovementKeys();
        this.updateRotationController();
        return true;
    }

    private boolean hasValidTarget() {
        return this.currentTarget != null && this.isValidTarget(this.currentTarget);
    }

    @EventHandler
    public void onPostAttack(EventPostAttack eventPostAttack) {
    }

    public SilentAuraRotationMode getRotationMode() {
        return this.rotationMode;
    }

    private void resetFlickState() {
        if (this.attackKeyHeld) {
            this.releaseAttackKey();
        }
        if (this.blinking || !this.heldPackets.isEmpty()) {
            this.flushHeldPackets();
        }
        this.attackQueued = false;
        this.attacked = false;
        this.attackKeyHeld = false;
        this.cancelSprint = false;
        this.sprintCancelPending = false;
        this.rotationMode = SilentAuraRotationMode.IDLE;
        this.stateTicks = 0;
        this.flickAngle = 0.0f;
        this.currentTarget = null;
        RotationManager.INSTANCE.setForwardMovementOverride(false);
        this.restoreBackKey();
        this.restoreForwardKey();
        this.rotationClaim.release(this);
        if (this.rotationController != null && RotationManager.INSTANCE.getActiveController() == this.rotationController) {
            this.rotationController.setScaleAxesProportionally(true);
            this.rotationController.setLinearAcceleration(true);
            RotationManager.INSTANCE.releaseController(this.rotationController);
        }
        this.rotationController = null;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onKeyPress(EventKeyPress eventKeyPress) {
        boolean attackKeyEvent = eventKeyPress.isKeybinding(Minecraft.gameSettings().F());
        boolean shouldHandle = attackKeyEvent && (Packet.A() || eventKeyPress.isDown());
        if (shouldHandle) {
            this.handleAttackInput(eventKeyPress);
        }
    }

    @EventHandler
    public void onPreAttack(EventPreAttack eventPreAttack) {
    }

    private void sendQueuedAttack() {
        if (!this.attackQueued || this.attacked) {
            return;
        }
        boolean attackHeld = AttackKeyController.requestSyntheticAttack(this);
        if (attackHeld) {
            AttackKeyController.releaseAttackKey();
        }
        this.attacked = true;
        this.attackKeyHeld = attackHeld;
        this.attackQueued = false;
    }

    private float getFlickAngle() {
        return this.flickAngle;
    }

    private void releaseBlinkClaim() {
        this.blinking = false;
        this.blinkTicks = 0;
        if (this.claimHeld) {
            this.primaryClaim.clearClaimed();
            this.claimHeld = false;
        }
    }

    private float computeFlickAwayRotationSpeed() {
        return this.scaleAngleToRotationSpeed(this.getTargetRotationError() + 2.0f, 1.0f);
    }

    private RotationAngles calculateTargetRotation() {
        if (this.currentTarget == null || this.rotationController == null) {
            return null;
        }
        double[] aimPoint = this.getTargetAimPoint(this.currentTarget);
        return this.rotationController.calculateRotation(Vec3.create(aimPoint[0], aimPoint[1], aimPoint[2]));
    }

    private boolean interceptAttackRequest() {
        if (this.rotationMode != SilentAuraRotationMode.IDLE) {
            return true;
        }
        if (!this.canStartFlick()) {
            return false;
        }
        RayTraceResult targetRayTrace = RotationManager.INSTANCE.getExtendedReachRayTrace();
        if (targetRayTrace.isNull() || targetRayTrace.getEntity().isNull() || !targetRayTrace.getEntity().isInstance(MappedClasses.zm)) {
            return false;
        }
        return this.startFlick(new EntityLivingBase(targetRayTrace.getEntity()));
    }

    private void forceForwardKey() {
        KeyBinding forwardKey = Minecraft.gameSettings().r();
        if (!ClientSettings.isPhysicalKeyDown(forwardKey)) {
            forwardKey.setPressed(true);
            this.forwardKeyForced = true;
        } else {
            this.forwardKeyForced = false;
        }
    }

    private void forceBackKey() {
        KeyBinding backKey = Minecraft.gameSettings().Y();
        if (!ClientSettings.isPhysicalKeyDown(backKey)) {
            backKey.setPressed(true);
            this.backKeyForced = true;
        } else {
            this.backKeyForced = false;
        }
    }

    public float getFlickAwayRotationSpeed() {
        return this.computeFlickAwayRotationSpeed();
    }


    private float getYawError(float targetYaw) {
        return Math.abs(MathUtil.wrapAngleTo180(RotationManager.INSTANCE.getManagedYaw() - targetYaw));
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onSyntheticAttackRequest(SyntheticAttackRequestEvent syntheticAttackRequestEvent) {
        if (syntheticAttackRequestEvent.getSource() == this) {
            return;
        }
        if (this.interceptAttackRequest()) {
            syntheticAttackRequestEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onMouseButton(EventMouseButton eventMouseButton) {
        boolean attackButtonEvent = eventMouseButton.isKeybinding(Minecraft.gameSettings().F());
        boolean shouldHandle = attackButtonEvent && (!Packet.h() || eventMouseButton.isDown());
        if (shouldHandle) {
            this.handleAttackInput(eventMouseButton);
        }
    }

    private boolean isAttackPacket(Packet packet) {
        if (!UseEntityPacketBridge.isUseEntityPacket(packet)) {
            return false;
        }
        return new UseEntityPacketBridge(packet).isAttack();
    }

    private boolean hasAllowedItem() {
        if (!this.limitToItems.getEffectiveValue().booleanValue()) {
            return true;
        }
        ItemStack itemStack = Minecraft.thePlayer().getHeldItemHand();
        return this.allowedItems.isValid(itemStack, false);
    }

    @Override
    public void onDisable() {
        this.resetFlickState();
        this.directionIndicatorUntil = 0L;
    }

    private float scaleAngleToRotationSpeed(float angle, float scale) {
        if (scale <= 0.0f) {
            return 0.0f;
        }
        return angle / (1.875f * scale);
    }

    @Override
    public String getDetailedSuffix() {
        String displayAngle = this.angle.getDisplayValue();
        if (this.rotationMode != SilentAuraRotationMode.IDLE) {
            displayAngle = "\u00a7c" + displayAngle;
        }
        return displayAngle + "deg";
    }

    private void restoreBackKey() {
        if (this.backKeyForced) {
            KeyBinding backKey = Minecraft.gameSettings().Y();
            backKey.setPressed(ClientSettings.isPhysicalKeyDown(backKey));
            this.backKeyForced = false;
        }
    }

    @EventHandler
    public void onTick(EventPrePlayerTick eventPrePlayerTick) {
        if (this.cancelSprint) {
            eventPrePlayerTick.getThePlayer().R(false);
            this.sprintCancelPending = true;
            this.cancelSprint = false;
        }
    }

    @EventHandler
    public void onSetSprinting(EventSetSprinting eventSetSprinting) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            this.sprintCancelPending = false;
            return;
        }
        if (this.sprintCancelPending && eventSetSprinting.isNewStateSprinting() && eventSetSprinting.getEntity().isNotNull() && eventSetSprinting.getEntity().S() == player.S()) {
            eventSetSprinting.setCancelled(true);
            this.sprintCancelPending = false;
        }
    }

    private EntityLivingBase getSilentAuraTargetUnderCrosshair() {
        if (this.silentAura == null) {
            this.silentAura = SonyVegas.INSTANCE.getModManager().getMod(SilentAura.class);
        }
        if (this.silentAura == null || !this.silentAura.isEnabled() || !this.silentAura.canClickAttack()) {
            return new EntityLivingBase(null);
        }
        EntityLivingBase target = this.silentAura.getTarget();
        if (target.isNull()) {
            return target;
        }
        RayTraceResult targetRayTrace = RotationManager.INSTANCE.getExtendedReachRayTrace();
        if (targetRayTrace.isNotNull() && targetRayTrace.getEntity().isNotNull() && target.equals(targetRayTrace.getEntity())) {
            return target;
        }
        return new EntityLivingBase(null);
    }

    private void updateMovementKeys() {
        if (this.rotationMode != SilentAuraRotationMode.ATTACKING) {
            RotationManager.INSTANCE.setForwardMovementOverride(false);
            this.restoreBackKey();
            this.restoreForwardKey();
            return;
        }
        this.forceBackKey();
        this.forceForwardKey();
        RotationManager.INSTANCE.setForwardMovementOverride(true);
    }

    private boolean isTargetVulnerable(EntityLivingBase target) {
        return target.c$src$I$15a9iwo() <= AttackPacketTimingTracker.INSTANCE.getExpectedHurtTimeTicks() + 1;
    }

    private float getPitchError(float targetPitch) {
        return Math.abs(MathUtil.wrapAngleTo180(RotationManager.INSTANCE.getManagedPitch() - targetPitch));
    }

    private float computeAttackRotationSpeed() {
        return Math.max(48.0f, this.scaleAngleToRotationSpeed(this.getAbsoluteFlickAngle(), 1.0f));
    }

    private void startBlink() {
        if (!this.blink.getEffectiveValue().booleanValue() || this.primaryClaim.isClaimed()) {
            return;
        }
        this.blinking = true;
        this.claimHeld = true;
        this.blinkTicks = 0;
        this.primaryClaim.markClaimed();
    }

    public float getAttackRotationSpeed() {
        return this.computeAttackRotationSpeed();
    }

    private void updateRotationController() {
        if (!this.hasValidTarget()) {
            this.resetFlickState();
            return;
        }
        if (this.rotationController == null) {
            this.rotationController = new HitFlickAdaptiveRotationController(this);
            this.rotationController.setRelativeMode(false);
            this.rotationController.setRetainAfterCompletion(true);
            this.rotationController.setClampStepToRemaining(true);
            this.rotationController.setTolerance(0.0f);
            this.rotationController.setScaleAxesProportionally(false);
            this.rotationController.setLinearAcceleration(false);
        }
        double[] aimPoint = this.getTargetAimPoint(this.currentTarget);
        this.rotationController.setTarget(aimPoint[0], aimPoint[1], aimPoint[2]);
        this.rotationController.setYawOffset(this.rotationMode == SilentAuraRotationMode.FLICKING_AWAY ? this.getFlickAngle() : 0.0f);
        this.rotationController.setRelativeMode(false);
        this.updateRotationRenderColors();
        if (RotationManager.INSTANCE.getActiveController() == null || RotationManager.INSTANCE.getActiveController() != this.rotationController) {
            RotationManager.INSTANCE.setController(this.rotationController);
        }
    }

    /**
     * Colors the shared aim indicator for the current flick phase, the same way
     * SilentAura tints it for its target. RotationManager draws whichever color
     * the active controller reports, so without this a flick showed up in
     * whatever color the previous owner of the rotation had left behind.
     */
    private void updateRotationRenderColors() {
        if (this.rotationController == null) {
            return;
        }
        this.rotationController.setPrimaryColor(new MutableColor(FLICK_AWAY_INDICATOR_COLOR));
        this.rotationController.setSecondaryColor(new MutableColor(FLICK_BACK_INDICATOR_COLOR));
        this.rotationController.setUseSecondaryColor(this.rotationMode == SilentAuraRotationMode.ATTACKING);
    }

    private float getTargetRotationError() {
        RotationAngles rotationAngles = this.calculateTargetRotation();
        if (rotationAngles == null) {
            return 0.0f;
        }
        float yawError = this.getYawError(rotationAngles.getYaw());
        float pitchError = this.getPitchError(rotationAngles.getPitch());
        return Math.max(yawError, pitchError);
    }

    private void flushHeldPackets() {
        if (this.heldPackets.isEmpty()) {
            this.releaseBlinkClaim();
            return;
        }
        this.heldPackets.forEach(this.dispatchGuard::o);
        this.heldPackets.clear();
        this.releaseBlinkClaim();
    }

    private void restoreForwardKey() {
        if (this.forwardKeyForced) {
            KeyBinding forwardKey = Minecraft.gameSettings().r();
            forwardKey.setPressed(ClientSettings.isPhysicalKeyDown(forwardKey));
            this.forwardKeyForced = false;
        }
    }

    @EventHandler
    public void onPacketSend(EventPacketSend eventPacketSend) {
        if (eventPacketSend.isCanceled() || eventPacketSend.wasModified()) {
            return;
        }
        Packet packet = eventPacketSend.getPacket();
        if (packet.isNull() || this.dispatchGuard.R(packet)) {
            return;
        }
        if (!this.blinking) {
            return;
        }
        this.heldPackets.add(eventPacketSend);
        eventPacketSend.setCancelled(true);
        if (this.isAttackPacket(packet)) {
            this.flushHeldPackets();
        }
    }

    public HitFlick() {
        super("KBDisplacement //$ui", -3580417, Category.GLADIADOR, "Flicks off and on target to displace knockback angle //$ui");
        this.angle = NumberValue.create(this, "Angle //$ui", "#", "deg", 0.0, 90.0, 180.0, 1.0, "0 = sem giro, 90 = direita, 180 = para trás //$ui");
        this.chance = NumberValue.create(this, "Chance //$ui", "#", "%", 0.0, 100.0, 100.0, 1.0, "Chance of starting a hit flick for a given attack //$ui");
        this.flickDelay = NumberValue.create(this, "Flick delay //$ui", "#", "ms", 0.0, 250.0, 2000.0, 25.0, "Minimum delay between hit flick attempts //$ui");
        this.webPriority = BooleanValue.create(this, "Web priority //$ui", false, "Prioritize flick towards nearest cobweb when available //$ui");
        this.webRange = NumberValue.create(this, "Web range //$ui", "#", "", 3.0, 6.0, 12.0, 1.0, "Search radius for cobwebs //$ui");
        this.webFov = NumberValue.create(this, "Web FOV //$ui", "#", "", 30.0, 60.0, 180.0, 5.0, "Max view angle to web for priority to trigger //$ui");
        this.randomizeOffset = BooleanValue.create(this, "Randomize offset //$ui", false, "Randomizes the configured angle by a per-flick range //$ui");
        this.randomizeOffsetRange = NumberValue.create(this, "Randomize offset //$ui", "#", "deg", 0.0, 0.0, 180.0, 1.0, "Applies a random offset range around Angle\nExample: 10 means Angle +/- 5 degrees per flick //$ui");
        this.strafeInvert = BooleanValue.create(this, "Strafe invert //$ui", false, "Flips the flick side when you strafe toward the current push direction //$ui");
        this.selectHits = BooleanValue.create(this, "Select hits //$ui", true, "Only start a hit flick when the target is vulnerable //$ui");
        this.limitToItems = BooleanValue.create(this, "Limit to items //$ui", false, "KBDisplacement functions only while holding selected items //$ui");
        this.allowedItems = LimitValue.create(this, "v-98c34d52 //$", "Allowed Items //$ui", LimitValue.ALLOW_LIST_COLOR, Arrays.asList(new ItemLimitData("swords //$")));
        this.rotationClaim = SharedModuleControlClaims.rotation;
        this.primaryClaim = SharedModuleControlClaims.primaryAction;
        this.dispatchGuard = PacketDispatchGuard.b;
        this.heldPackets = new LinkedList<EventPacketSend>();
        this.flickDelayTimer = new TimerUtil();
        this.rotationMode = SilentAuraRotationMode.IDLE;
        this.randomizeOffset.addDependentValues(this.randomizeOffsetRange);
        this.webPriority.addDependentValues(this.webRange, this.webFov);
        this.limitToItems.addDependentValues(this.allowedItems);
        this.limitToItems.setCompactListValue(this.allowedItems);
        this.addValue(this.targetFilter, this.angle, this.chance, this.flickDelay, this.randomizeOffset, this.randomizeOffsetRange, this.strafeInvert, this.selectHits, this.webPriority, this.webRange, this.webFov, this.blink, this.showDirection, this.directionDuration, this.directionColor, this.limitToItems, this.allowedItems);
        this.chance.setMaximumFractionDigits(0);
        this.flickDelayTimer.x(-10000L);
        this.rotationClaim.setPriority(this, 6);
    }

    private void showDirectionIndicator(EntityLivingBase target) {
        if (!this.showDirection.getEffectiveValue() || Math.abs(MathUtil.wrapAngleTo180(this.flickAngle)) <= 0.01f) {
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || target == null || target.isNull()) return;
        double deltaX = target.z() - player.z();
        double deltaZ = target.h() - player.h();
        if (deltaX * deltaX + deltaZ * deltaZ < 1.0E-6) return;
        this.directionIndicatorYaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI)
                - 90.0f + this.flickAngle;
        this.directionIndicatorUntil = System.currentTimeMillis()
                + this.directionDuration.getValue().longValue();
    }

    /** A short world-space arrow beside the local player, pointing where this
     * flick will orient the hit.  It uses the same camera-relative GL setup as
     * Basquete, so blocks naturally hide it rather than drawing through walls. */
    @EventHandler
    public void onRender3D(EventRender3D event) {
        long now = System.currentTimeMillis();
        if (!this.showDirection.getEffectiveValue() || now >= this.directionIndicatorUntil) return;
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) return;

        float life = Math.max(0.0f, Math.min(1.0f,
                (this.directionIndicatorUntil - now) / this.directionDuration.getValue().floatValue()));
        float alpha = 0.25f + life * 0.7f;
        double radians = Math.toRadians(this.directionIndicatorYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double startX = player.z() + forwardX * 0.9;
        double startY = player.N() + 1.05;
        double startZ = player.h() + forwardZ * 0.9;
        double endX = startX + forwardX * 0.92;
        double endZ = startZ + forwardZ * 0.92;
        double cameraX = RenderManager.getInterpolatedRenderPosX();
        double cameraY = RenderManager.getInterpolatedRenderPosY();
        double cameraZ = RenderManager.getInterpolatedRenderPosZ();
        Color color = this.directionColor.getMutableColor();

        RenderUtil.d();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f,
                    color.getBlue() / 255.0f, alpha);
            GL11.glLineWidth(2.4f);
            GL11.glBegin(GL11.GL_LINES);
            worldVertex(startX, startY, startZ, cameraX, cameraY, cameraZ);
            worldVertex(endX, startY, endZ, cameraX, cameraY, cameraZ);
            GL11.glEnd();

            double headBaseX = endX - forwardX * 0.25;
            double headBaseZ = endZ - forwardZ * 0.25;
            GL11.glBegin(GL11.GL_TRIANGLES);
            worldVertex(endX + forwardX * 0.16, startY, endZ + forwardZ * 0.16,
                    cameraX, cameraY, cameraZ);
            worldVertex(headBaseX + sideX * 0.27, startY, headBaseZ + sideZ * 0.27,
                    cameraX, cameraY, cameraZ);
            worldVertex(headBaseX - sideX * 0.27, startY, headBaseZ - sideZ * 0.27,
                    cameraX, cameraY, cameraZ);
            GL11.glEnd();
        } finally {
            GL11.glDepthMask(true);
            GL11.glPopAttrib();
            RenderUtil.Y();
        }
    }

    private static void worldVertex(double x, double y, double z,
                                    double cameraX, double cameraY, double cameraZ) {
        GL11.glVertex3d(x - cameraX, y - cameraY, z - cameraZ);
    }

    private void handleAttackInput(EventKeyInputBase inputEvent) {
        if (this.rotationMode != SilentAuraRotationMode.IDLE) {
            inputEvent.setCancelled(true);
            return;
        }
        if (!this.canStartFlick()) {
            return;
        }
        RayTraceResult targetRayTrace = RotationManager.INSTANCE.getExtendedReachRayTrace();
        if (targetRayTrace.isNull() || targetRayTrace.getEntity().isNull() || !targetRayTrace.getEntity().isInstance(MappedClasses.zm)) {
            return;
        }
        if (this.startFlick(new EntityLivingBase(targetRayTrace.getEntity()))) {
            inputEvent.setCancelled(true);
        }
    }

    private boolean passesChanceRoll() {
        return (Double)this.chance.getValue() >= Math.random() * 100.0;
    }

    private boolean hasReachedFlickRotation() {
        RotationAngles rotationAngles = this.calculateTargetRotation();
        if (rotationAngles == null) {
            return false;
        }
        float offsetYaw = rotationAngles.getYaw() + this.getFlickAngle();
        float yawError = this.getYawError(offsetYaw);
        float pitchError = this.getPitchError(rotationAngles.getPitch());
        return Math.max(yawError, pitchError) <= 6.0f;
    }

    private float getRandomizedFlickAngle() {
        if (!this.randomizeOffset.getEffectiveValue().booleanValue()) {
            return this.getConfiguredFlickAngle();
        }
        float offsetRange = ((Double)this.randomizeOffsetRange.getValue()).floatValue();
        if (offsetRange <= 0.0f) {
            return this.getConfiguredFlickAngle();
        }
        float randomOffset = (ThreadLocalRandom.current().nextFloat() - 0.5f) * offsetRange;
        return this.normalizeAngle(((Double)this.angle.getValue()).floatValue() + randomOffset);
    }

    private float normalizeAngle(float angleDegrees) {
        float normalized = angleDegrees % 360.0f;
        if (normalized < 0.0f) {
            normalized += 360.0f;
        }
        return normalized;
    }

    private void releaseAttackKey() {
        AttackKeyController.releaseAttackKey();
        this.attackKeyHeld = false;
    }

    private float resolveStrafeAdjustedAngle() {
        Float webAngle = this.getWebFlickAngle();
        if (webAngle != null) {
            return webAngle;
        }
        float flickAngle = this.getRandomizedFlickAngle();
        if (!this.strafeInvert.getEffectiveValue().booleanValue()) {
            return flickAngle;
        }
        boolean leftPressed = ClientSettings.isPhysicalKeyDown(Minecraft.gameSettings().x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg());
        boolean rightPressed = ClientSettings.isPhysicalKeyDown(Minecraft.gameSettings().g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3());
        if (leftPressed == rightPressed) {
            return flickAngle;
        }
        float signedAngle = MathUtil.wrapAngleTo180(flickAngle);
        if (Math.abs(signedAngle) <= 0.001f || Math.abs(Math.abs(signedAngle) - 180.0f) <= 0.001f) {
            return flickAngle;
        }
        if (leftPressed && signedAngle < 0.0f || rightPressed && signedAngle > 0.0f) {
            return this.normalizeAngle(-signedAngle);
        }
        return flickAngle;
    }

    private Float getWebFlickAngle() {
        if (!this.webPriority.getEffectiveValue().booleanValue()) {
            return null;
        }
        BlockPos nearestWeb = this.findNearestWeb();
        if (nearestWeb == null || this.currentTarget == null || this.currentTarget.isNull()) {
            return null;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return null;
        }
        double webX = nearestWeb.getX() + 0.5;
        double webZ = nearestWeb.getZ() + 0.5;
        double playerX = player.z();
        double playerZ = player.h();
        float yawToWeb = (float)(Math.atan2(webZ - playerZ, webX - playerX) * 180.0 / Math.PI) - 90.0f;
        float playerYaw = MEntity.l$src$F$kucftj(Minecraft.sonyVegasInstance.getMappings().Rr, player.getObject());
        try {
            float managedYaw = RotationManager.INSTANCE.getManagedYaw();
            if (!Float.isNaN(managedYaw) && RotationManager.INSTANCE.getActiveController() != null) {
                playerYaw = managedYaw;
            }
        } catch (Throwable ignored) {}
        float fov = this.webFov.getValue().floatValue();
        float diffToWeb = Math.abs(MathUtil.wrapAngleTo180(yawToWeb - playerYaw));
        if (diffToWeb > fov) {
            return null;
        }
        double[] aimPoint = this.getTargetAimPoint(this.currentTarget);
        double targetX = aimPoint[0];
        double targetZ = aimPoint[2];
        float yawToTarget = (float)(Math.atan2(targetZ - playerZ, targetX - playerX) * 180.0 / Math.PI) - 90.0f;
        float delta = MathUtil.wrapAngleTo180(yawToWeb - yawToTarget);
        return this.normalizeAngle(delta);
    }

    private BlockPos findNearestWeb() {
        if (Minecraft.theWorld().isNull() || Minecraft.thePlayer().isNull()) {
            return null;
        }
        Block webBlock = Blocks.web();
        boolean hasWebMapping = webBlock != null && !webBlock.isNull() && webBlock.getObject() != null;
        World world = Minecraft.theWorld();
        EntityPlayerSP player = Minecraft.thePlayer();
        double px = player.z();
        double py = player.N();
        double pz = player.h();
        int range = this.webRange.getValue().intValue();
        int yRange = 3;
        BlockPos nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int dx = -range; dx <= range; ++dx) {
            for (int dy = -yRange; dy <= yRange; ++dy) {
                for (int dz = -range; dz <= range; ++dz) {
                    BlockPos pos = BlockPos.create((int)Math.floor(px + dx), (int)Math.floor(py + dy), (int)Math.floor(pz + dz));
                    try {
                        BlockState state = world.getBlockState(pos);
                        if (state == null || state.isNull()) continue;
                        Block block = state.getBlock();
                        if (block == null || block.isNull()) continue;
                        boolean isWeb = false;
                        if (hasWebMapping) {
                            try {
                                isWeb = block.getObject().equals(webBlock.getObject());
                                if (!isWeb) {
                                    String unloc = block.U();
                                    if (unloc != null) isWeb = unloc.toLowerCase(java.util.Locale.ROOT).contains("web");
                                }
                            } catch (Throwable t) {
                                isWeb = false;
                            }
                        }
                        if (!isWeb) {
                            try {
                                String cls = block.getObject().getClass().getName().toLowerCase(java.util.Locale.ROOT);
                                isWeb = cls.contains("web");
                            } catch (Throwable ignored) {}
                            if (!isWeb) {
                                try {
                                    String unloc = block.U();
                                    if (unloc != null) isWeb = unloc.toLowerCase(java.util.Locale.ROOT).contains("web");
                                } catch (Throwable ignored) {}
                            }
                        }
                        if (!isWeb) continue;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            nearest = pos;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
        return nearest;
    }

    private float getAbsoluteFlickAngle() {
        return Math.abs(MathUtil.wrapAngleTo180(this.getFlickAngle()));
    }

    private boolean canStartFlick() {
        if (!this.hasConfiguredAngle()) {
            return false;
        }
        if (Minecraft.theWorld().isNull() || Minecraft.thePlayer().isNull()) {
            return false;
        }
        if (Minecraft.currentScreen().isNotNull()) {
            return false;
        }
        if (!this.hasAllowedItem()) {
            return false;
        }
        return this.flickDelayTimer.hasTimeElapsed(((Double)this.flickDelay.getValue()).longValue());
    }

    private float getConfiguredFlickAngle() {
        return this.normalizeAngle(((Double)this.angle.getValue()).floatValue());
    }

    private boolean hasConfiguredAngle() {
        if (this.webPriority != null && this.webPriority.getEffectiveValue().booleanValue()) {
            BlockPos web = this.findNearestWeb();
            if (web != null && this.isWebWithinFov(web)) {
                return true;
            }
        }
        return Math.abs(MathUtil.wrapAngleTo180(this.getConfiguredFlickAngle())) > 0.001f
                || this.randomizeOffset.getEffectiveValue() != false && ((Double)this.randomizeOffsetRange.getValue()).floatValue() > 0.001f;
    }

    private boolean isWebWithinFov(BlockPos webPos) {
        if (webPos == null || Minecraft.thePlayer().isNull()) return false;
        EntityPlayerSP player = Minecraft.thePlayer();
        double webX = webPos.getX() + 0.5;
        double webZ = webPos.getZ() + 0.5;
        double playerX = player.z();
        double playerZ = player.h();
        float yawToWeb = (float)(Math.atan2(webZ - playerZ, webX - playerX) * 180.0 / Math.PI) - 90.0f;
        float playerYaw = MEntity.l$src$F$kucftj(Minecraft.sonyVegasInstance.getMappings().Rr, player.getObject());
        try {
            float managedYaw = RotationManager.INSTANCE.getManagedYaw();
            if (!Float.isNaN(managedYaw) && RotationManager.INSTANCE.getActiveController() != null) {
                playerYaw = managedYaw;
            }
        } catch (Throwable ignored) {}
        float fov = this.webFov.getValue().floatValue();
        return Math.abs(MathUtil.wrapAngleTo180(yawToWeb - playerYaw)) <= fov;
    }

    @EventHandler(priority=EventPriority.LOWEREST)
    public void onTick(EventPreTick eventPreTick) {
        if (Minecraft.currentScreen().isNotNull() || eventPreTick.getThePlayer().isNull()) {
            this.resetFlickState();
            return;
        }
        if (this.rotationMode == SilentAuraRotationMode.IDLE) {
            return;
        }
        if (!this.hasValidTarget()) {
            this.resetFlickState();
            return;
        }
        if (!this.rotationClaim.isOwnedBy(this) && !this.rotationClaim.acquire(this, true)) {
            this.resetFlickState();
            return;
        }
        if (this.blinking && ++this.blinkTicks >= 7) {
            this.flushHeldPackets();
        }
        ++this.stateTicks;
        switch (SilentAuraAdaptiveRotationEntry.MODE_ORDINALS[this.rotationMode.ordinal()]) {
            case 1: {
                if (this.stateTicks < 5 && !this.hasReachedFlickRotation()) break;
                this.rotationMode = SilentAuraRotationMode.ATTACKING;
                this.stateTicks = 0;
                break;
            }
            case 2: {
                if (!this.attacked) {
                    this.sendQueuedAttack();
                }
                if (this.stateTicks < 2) break;
                this.resetFlickState();
            }
        }
        this.updateMovementKeys();
        this.updateRotationController();
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onClickMouse(EventClickMouse eventClickMouse) {
        if (this.rotationMode != SilentAuraRotationMode.IDLE) {
            return;
        }
        EntityLivingBase target = this.getSilentAuraTargetUnderCrosshair();
        if (target.isNotNull() && this.startFlick(target)) {
            eventClickMouse.setCancelled(true);
        }
    }
}
