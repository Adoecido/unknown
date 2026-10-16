package vegas.sony.module.rise;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventMotion;
import vegas.sony.event.impl.EventPreLocalPlayerTick;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.Reach;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.rotation.PlayerMouseRotationApplier;
import vegas.sony.rotation.RotationControlClaim;
import vegas.sony.rotation.RotationManager;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.MathUtil;
import vegas.sony.utils.RayTraceUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.utils.Vec3d;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.Vec3;
import vegas.sony.wrapper.impl.WorldClient;

public final class RiseAim extends Mod {
    private final ModeOption legitMode = new ModeOption("Legit //$ui");
    private final ModeOption blatantMode = new ModeOption("Blatant //$ui");
    private final ModeValue mode = ModeValue.create(this, "Mode //$ui", this.legitMode,
            this.legitMode, this.blatantMode);
    private final NumberValue speed = NumberValue.create(this, "Speed //$ui", "#", "",
            1.0, 2.0, 180.0, 1.0);
    private final BooleanValue silent = BooleanValue.create(this, "Silent //$ui", false);
    private final BooleanValue requireSwinging = BooleanValue.create(this, "Require Swinging //$ui", true);
    private final BooleanValue sticky = BooleanValue.create(this, "Sticky //$ui", false);
    private final BooleanValue requireMouseMovement = BooleanValue.create(this,
            "Require Mouse Movement //$ui", false);
    private final BooleanValue limitItems = BooleanValue.create(this, "Limit Items //$ui", false,
            "Only aims while holding a sword //$ui");
    private final BooleanValue aimWhilstOnTarget = BooleanValue.create(this,
            "Aim Whilst on Target //$ui", false);
    private final NumberValue fov = NumberValue.create(this, "FOV //$ui", "#", "° //$ui",
            0.0, 90.0, 360.0, 1.0);
    private final BooleanValue players = BooleanValue.create(this, "Players //$ui", true);
    private final BooleanValue invisibles = BooleanValue.create(this, "Invisibles //$ui", false);
    private final BooleanValue animals = BooleanValue.create(this, "Animals //$ui", false);
    private final BooleanValue mobs = BooleanValue.create(this, "Mobs //$ui", false);
    private final BooleanValue playerTeammates = BooleanValue.create(this, "Player Teammates //$ui", true);
    private final RotationControlClaim rotationClaim = SharedModuleControlClaims.rotation;
    private EntityLivingBase target;
    private float silentYaw;
    private float silentPitch;
    private boolean silentRotationActive;

    public RiseAim() {
        super("RiseAim //$ui", 9133823, Category.COMBAT,
                "Rise 6.9.5 aim assist with visible and silent rotation paths //$ui");
        this.speed.setMaximumFractionDigits(0);
        this.fov.setMaximumFractionDigits(0);
        this.rotationClaim.setPriority(this, 4);
        this.addValue(this.mode, this.speed, this.silent, this.requireSwinging, this.sticky,
                this.requireMouseMovement, this.limitItems, this.aimWhilstOnTarget, this.fov,
                this.players, this.invisibles, this.animals, this.mobs, this.playerTeammates);
    }

    @EventHandler
    public void onLocalPlayerTick(EventPreLocalPlayerTick event) {
        EntityPlayerSP player = event.getPlayer();
        if (!this.canAim(player)) {
            this.clearTarget();
            return;
        }
        double range = this.getRange();
        EntityLivingBase selected = this.findNearestTarget(range);
        if (selected == null) {
            this.clearTarget();
            return;
        }
        float viewYaw = RotationManager.getViewYaw(player);
        float viewPitch = RotationManager.getViewPitch(player);
        boolean piercingEnabled = this.isPiercingEnabled();
        double[] aimPoint = this.resolveAimPoint(player, selected);
        float[] targetRotation = calculateRotations(player, aimPoint[0], aimPoint[1], aimPoint[2]);
        float appliedPitch = targetRotation[1];
        if (this.mode.getValue() == this.legitMode && !piercingEnabled) {
            appliedPitch = this.silent.getEffectiveValue().booleanValue()
                    ? (this.silentRotationActive
                    ? this.silentPitch : RotationManager.INSTANCE.getManagedPitch())
                    : viewPitch;
        }
        float yawDifference = MathUtil.wrapAngleTo180(targetRotation[0] - viewYaw);
        float absoluteYawDifference = Math.abs(yawDifference);
        if (absoluteYawDifference > this.fov.getValue().floatValue()
                || !this.rotationWouldHit(player, selected, targetRotation[0], targetRotation[1], range)) {
            this.clearTarget();
            return;
        }
        this.target = selected;
        double baseSpeed = this.speed.getValue().doubleValue()
                * (this.sticky.getEffectiveValue().booleanValue() ? 10.0 : 1.0);
        double angleFactor = Math.min(1.0, absoluteYawDifference / 45.0);
        double finalSpeed = Math.max(0.05, baseSpeed * (0.35 + angleFactor * 0.65));
        float interpolation = (float)Math.min(1.0, finalSpeed / 36.0);
        if (this.silent.getEffectiveValue().booleanValue()) {
            this.applySilentRotation(player, targetRotation[0], appliedPitch, interpolation);
        } else {
            this.disableSilentRotation();
            this.applyVisibleRotation(player, targetRotation[0], appliedPitch, interpolation);
        }
    }

    private boolean canAim(EntityPlayerSP player) {
        if (player.isNull() || Minecraft.theWorld().isNull() || Minecraft.currentScreen().isNotNull()) {
            return false;
        }
        if (this.requireSwinging.getEffectiveValue().booleanValue() && !player.Y$src$Z$154rldp()) {
            return false;
        }
        if (this.requireMouseMovement.getEffectiveValue().booleanValue()
                && Minecraft.p().getDeltaX() == 0 && Minecraft.p().getDeltaY() == 0) {
            return false;
        }
        if (this.limitItems.getEffectiveValue().booleanValue()) {
            ItemStack held = player.getHeldItemHand();
            if (held.isNull() || held.getItem().isNull() || !held.getItem().isInstance(MappedClasses.V5)) {
                return false;
            }
        }
        RayTraceResult mouseOver = RayTraceUtil.o();
        boolean holdingClick = Minecraft.gameSettings().F().isKeyDown()
                || Minecraft.gameSettings().b$src$Lvegas_sony_wrapper_impl_KeyBinding_$1yi3362().isKeyDown();
        if (holdingClick && mouseOver.isNotNull() && mouseOver.isBlockHit()) {
            return false;
        }
        return this.aimWhilstOnTarget.getEffectiveValue().booleanValue()
                || mouseOver.isNull() || !mouseOver.isEntityHit();
    }

    private double getRange() {
        RisePiercing piercing = this.getPiercing();
        if (piercing != null && piercing.isEnabled()) {
            return Math.max(4.0, piercing.getReachRange());
        }
        Reach reach = SonyVegas.INSTANCE.getModManager().getMod(Reach.class);
        return reach != null && reach.isEnabled()
                ? Math.max(4.0, reach.getMaximumConfiguredReachDistance()) : 4.0;
    }

    private EntityLivingBase findNearestTarget(double range) {
        WorldClient world = Minecraft.theWorld();
        List<EntityLivingBase> candidates = new ArrayList<EntityLivingBase>();
        for (Object handle : world.z()) {
            Entity entity = new Entity(handle);
            if (!entity.isInstance(MappedClasses.zm)) {
                continue;
            }
            EntityLivingBase living = new EntityLivingBase(handle);
            if (this.isValidTarget(living, range)) {
                candidates.add(living);
            }
        }
        candidates.sort(Comparator.comparingDouble(candidate ->
                Minecraft.thePlayer().getDistanceToEntity(candidate)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean isValidTarget(EntityLivingBase living, double range) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (living.isNull() || living.equals(player) || living.M$src$Z$ff28xj()
                || living.w$src$F$15l9epb() <= 0.0f
                || player.getDistanceToEntity(living) > range
                || SonyVegas.INSTANCE.getFriendManager().isFriend(living)) {
            return false;
        }
        if (!this.invisibles.getEffectiveValue().booleanValue() && RotationUtil.k(living)) {
            return false;
        }
        boolean isPlayer = living.isInstance(MappedClasses.lG);
        if (isPlayer) {
            if (!this.players.getEffectiveValue().booleanValue()) {
                return false;
            }
            return this.playerTeammates.getEffectiveValue().booleanValue()
                    || !SonyVegas.INSTANCE.getClientSettings().isTeammate(player, living);
        }
        boolean peaceful = living.isInstance(MappedClasses.ZP)
                || living.isInstance(MappedClasses.Fb) || living.isInstance(MappedClasses.qf);
        if (peaceful) {
            return this.animals.getEffectiveValue().booleanValue();
        }
        Class mobClass = MappedClasses.Fr;
        boolean hostile = living.isInstance(mobClass) || living.isInstance(MappedClasses.Zo)
                || living.isInstance(MappedClasses.ur) || living.isInstance(MappedClasses.Z8);
        return hostile && this.mobs.getEffectiveValue().booleanValue();
    }

    private double[] resolveAimPoint(EntityPlayerSP player, EntityLivingBase living) {
        if (this.mode.getValue() == this.blatantMode || this.isPiercingEnabled()) {
            Vec3d closest = RotationUtil.T(player, this.getAimBox(living), 0.0, 0.0, 0.0);
            return new double[]{closest.getX(), closest.getY(), closest.getZ()};
        }
        AxisAlignedBB box = living.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl();
        return new double[]{living.z(), (box.getMinY() + box.getMaxY()) * 0.5, living.h()};
    }

    private static float[] calculateRotations(EntityPlayerSP player, double x, double y, double z) {
        double deltaX = x - player.z();
        double deltaY = y - (player.N() + (double)player.X());
        double deltaZ = z - player.h();
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        return new float[]{
                (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f,
                (float)-Math.toDegrees(Math.atan2(deltaY, Math.max(horizontal, 1.0E-4)))
        };
    }

    private boolean rotationWouldHit(EntityPlayerSP player, EntityLivingBase living,
            float yaw, float pitch, double range) {
        Vec3 origin = Vec3.create(player.z(), player.N() + (double)player.X(), player.h());
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRadians);
        Vec3 end = origin.addVector(-Math.sin(yawRadians) * cosPitch * range,
                -Math.sin(pitchRadians) * range,
                Math.cos(yawRadians) * cosPitch * range);
        RayTraceResult entityHit = this.getAimBox(living).expand(0.1, 0.1, 0.1)
                .calculateIntercept(origin, end);
        if (entityHit == null || entityHit.isNull()) {
            return false;
        }
        if (this.isPiercingEnabled()) {
            return true;
        }
        RayTraceResult blockHit = Minecraft.theWorld().K(origin, end, false, false, false, player);
        return blockHit == null || blockHit.isNull()
                || origin.distanceTo(entityHit.getHitVec()) <= origin.distanceTo(blockHit.getHitVec());
    }

    private AxisAlignedBB getAimBox(EntityLivingBase living) {
        AxisAlignedBB box = living.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl();
        RisePiercing piercing = this.getPiercing();
        if (piercing != null && piercing.isEnabled()) {
            float expansion = piercing.getHitBoxExpand();
            return expansion > 0.0f ? box.expand(expansion, expansion, expansion) : box;
        }
        if (this.mode.getValue() != this.blatantMode) {
            return box;
        }
        return box;
    }

    private RisePiercing getPiercing() {
        return SonyVegas.INSTANCE.getModManager().getMod(RisePiercing.class);
    }

    private boolean isPiercingEnabled() {
        RisePiercing piercing = this.getPiercing();
        return piercing != null && piercing.isEnabled();
    }

    private void applyVisibleRotation(EntityPlayerSP player, float targetYaw, float targetPitch,
            float interpolation) {
        float currentYaw = RotationManager.getViewYaw(player);
        float currentPitch = RotationManager.getViewPitch(player);
        float yawStep = MathUtil.wrapAngleTo180(targetYaw - currentYaw) * interpolation;
        float pitchStep = MathUtil.wrapAngleTo180(targetPitch - currentPitch) * interpolation;
        PlayerMouseRotationApplier.applyTrackedMouseDelta(yawStep / 0.15f, -pitchStep / 0.15f);
    }

    private void applySilentRotation(EntityPlayerSP player, float targetYaw, float targetPitch,
            float interpolation) {
        if (!this.rotationClaim.isOwnedBy(this) && !this.rotationClaim.acquire(this, true)) {
            this.silentRotationActive = false;
            return;
        }
        if (!this.silentRotationActive) {
            this.silentYaw = RotationManager.INSTANCE.getManagedYaw();
            this.silentPitch = RotationManager.INSTANCE.getManagedPitch();
        }
        this.silentYaw += MathUtil.wrapAngleTo180(targetYaw - this.silentYaw) * interpolation;
        this.silentPitch += MathUtil.wrapAngleTo180(targetPitch - this.silentPitch) * interpolation;
        this.silentPitch = MathUtil.clamp(this.silentPitch, -90.0f, 90.0f);
        this.silentRotationActive = true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreMotion(EventPreMotion event) {
        if (this.silentRotationActive && this.rotationClaim.isOwnedBy(this)) {
            EventMotion.setRotationYaw(this.silentYaw);
            EventMotion.setRotationPitch(this.silentPitch);
        }
    }

    private void disableSilentRotation() {
        if (this.rotationClaim.isOwnedBy(this)) {
            this.rotationClaim.release(this);
        }
        this.silentRotationActive = false;
    }

    private void clearTarget() {
        this.target = null;
        this.disableSilentRotation();
    }

    public EntityLivingBase getTarget() {
        return this.target;
    }

    @Override
    public String getSimpleSuffix() {
        return this.mode.getDisplayValue();
    }

    @Override
    public void onDisable() {
        this.clearTarget();
    }
}
