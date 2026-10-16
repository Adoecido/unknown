package vegas.sony.module.combat;

import vegas.sony.SonyVegas;
import vegas.sony.combat.EntityWebDetection;
import vegas.sony.combat.PlayerVisibility;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.event.impl.EventSendClickBlockToController;
import vegas.sony.event.impl.SyntheticAttackRequestEvent;
import vegas.sony.input.AttackKeyController;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.KeyBindingHelper;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.silentaura.SilentAuraAimJitter;
import vegas.sony.module.combat.silentaura.AuraAttackTiming;
import vegas.sony.module.combat.silentaura.SilentAuraClicker;
import vegas.sony.module.combat.silentaura.SilentAuraRotationController;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.module.gladiador.Target;
import vegas.sony.rotation.AdaptiveRotationController;
import vegas.sony.rotation.MouseRotationController;
import vegas.sony.rotation.RotationAngles;
import vegas.sony.rotation.RotationControlClaim;
import vegas.sony.rotation.RotationManager;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.utils.EntityAngleComparator;
import vegas.sony.utils.EntityArmorValueComparator;
import vegas.sony.utils.EntityDistanceComparator;
import vegas.sony.utils.EntityEquipmentValueComparator;
import vegas.sony.utils.EntityHealthComparator;
import vegas.sony.utils.EntityScoreboardHealthComparator;
import vegas.sony.utils.MathUtil;
import vegas.sony.utils.RayTraceUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.render.JelloTargetMarkRenderer;
import vegas.sony.utils.render.RenderUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.EntityTargetFilterValue;
import vegas.sony.value.LimitValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.RandomClickDelayValue;
import vegas.sony.value.RandomValue;
import vegas.sony.value.Value;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GuiScreen;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.Score;
import vegas.sony.wrapper.impl.ScoreObjective;
import vegas.sony.wrapper.impl.Scoreboard;
import vegas.sony.wrapper.impl.TitledScreen;
import vegas.sony.wrapper.impl.UseEntityPacketBridge;
import vegas.sony.wrapper.impl.Vec3;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SilentAura
extends Mod {
    /**
     * Extra swing extends acquisition, not attack reach.
     * Actual attacks are accepted through exactly 3.0 blocks, with no 2.88 cap.
     */
    private static final double ACQUISITION_REACH = 3.0;
    private static final long PENDING_AURA_ATTACK_TIMEOUT_MS = 2000L;
    private final ModeOption distanceMode;
    private long nextBreakBlockTime = 0L;
    public final ModeOption armorMode;
    public final BooleanValue showTarget;
    private boolean perfectSwingAttackPending = false;
    private final RandomClickDelayValue attackRate;
    private final AuraAttackTiming attackTiming = new AuraAttackTiming();
    private volatile int legitSwingTargetId = Integer.MIN_VALUE;
    private final ModeOption legitHitMode;
    private final ModeOption blatantHitMode;
    private final ModeValue hitMode;
    private final BooleanValue failSwing;
    private final BooleanValue forceFirstHit;
    private volatile int failSwingTargetId = Integer.MIN_VALUE;
    private boolean firstHitPending;
    private volatile boolean attackClickSuppressed;
    private boolean endingBlockBreaking;
    private float yawIntegralScale = 1.0f;
    public final ModeOption threatMode;
    public final NumberValue maxAngle;
    private AdaptiveRotationController rotationController;
    public final EntityTargetFilterValue targetFilter = EntityTargetFilterValue.createForModule(this);
    private final BooleanValue attackBots = BooleanValue.create(this,
            "Attack bots //$ui", true,
            "Allows visible player entities that are not listed in the tab list //$ui");
    private final BooleanValue ignoreInWeb = BooleanValue.create(this,
            "Ignore in Web //$ui", false,
            "Ignores targets with their body inside cobwebs; edge contact still allows attacks //$ui");
    private final BooleanValue switchTargets;
    public final NumberValue switchDelay;
    private boolean hitCurrentTarget = false;
    private final ArrayList<Integer> prevTargetIds = new ArrayList<Integer>();
    private final ArrayList<EntityLivingBase> renderTargets = new ArrayList<EntityLivingBase>();
    private int pendingSwitchTargetId = Integer.MIN_VALUE;
    private long pendingSwitchAt = 0L;
    private final BooleanValue limitToItems;
    public final ModeOption yawMode;
    private final RotationControlClaim rotationClaim;
    private EntityLivingBase target = null;
    /** The target captured when Aura requests a synthetic click. */
    private volatile EntityLivingBase pendingAuraAttackTarget;
    private volatile long pendingAuraAttackExpiresAt;
    private static SilentAuraClicker clicker;
    private final RandomValue breakBlocksDelay;
    private boolean readyToAttack;
    public final ModeOption ringMode;
    private final BooleanValue requireMouseDown;
    private final LimitValue allowedItems;
    private float yawIntegral = 0.0f;
    private float pitchIntegral = 0.0f;
    private final BooleanValue perfectSwing;
    private float pitchProportionalScale = 1.0f;
    private final ColorValue attackColor;
    private final BooleanValue syncMarkWithHit = BooleanValue.create(this,
            "Sync mark with hit //$ui", true,
            "Changes the target mark to Attack color only after a real attack packet //$ui");
    private final NumberValue aimSpeed = NumberValue.createWithDescription(this, "Aim speed //$ui", "#.#", "", 1.0, 7.0, 20.0, "How fast aiming will be done silently //$ui");
    public final ModeOption healthMode;
    private boolean onTarget = false;
    private final BooleanValue breakBlocksWhitelist;
    public final BooleanValue disableOnDeath;
    private boolean breakingBlocks;
    public final ModeOption boxMode;
    public final NumberValue extraSwingDistance = NumberValue.createWithDescription(this, "Extra swing distance //$ui", "#.#", "", 0.0, 1.0, 3.0, "Extra distance used to acquire and aim at a target; attacks stop at 3 blocks //$ui");
    private final BooleanValue breakBlocks;
    private final LimitValue blockBreakItems;
    private final ColorValue targetColor;
    private final NumberValue jelloWidth;
    private final NumberValue jelloThickness;
    private final NumberValue jelloHeight;
    private final NumberValue jelloOffset;
    private final NumberValue jelloSpeed;
    private final NumberValue jelloOpacity;
    private final TimerUtil breakBlocksTimer;
    public ModeValue renderType;
    private float yawProportionalScale = 1.0f;
    private float pitchIntegralScale = 1.0f;
    private static final long MODULE_ID;
    public ModeValue targetMode;
    private boolean deathHandled = false;

    public EntityLivingBase getTarget() {
        return this.isIgnoredInWeb(this.target) ? null : this.target;
    }

    public RotationControlClaim getRotationClaim() {
        return this.rotationClaim;
    }

    public NumberValue getAimSpeed() {
        return this.aimSpeed;
    }

    public RandomClickDelayValue getAttackRate() {
        return this.attackRate;
    }

    public BooleanValue getLimitToItems() {
        return this.limitToItems;
    }

    public LimitValue getAllowedItems() {
        return this.allowedItems;
    }

    public BooleanValue getRequireMouseDown() {
        return this.requireMouseDown;
    }

    @EventHandler
    public void onRender3D(EventRender3D eventRender3D) {
        if (!this.showTarget.getEffectiveValue().booleanValue() || Minecraft.currentScreen().isNotNull()) {
            return;
        }
        ArrayList<EntityLivingBase> visibleTargets = new ArrayList<EntityLivingBase>(this.renderTargets);
        for (EntityLivingBase candidate : visibleTargets) {
            if (candidate == null || candidate.isNull() || this.isIgnoredInWeb(candidate)) continue;
            boolean attackedTarget = this.syncMarkWithHit.getEffectiveValue()
                    && this.hitCurrentTarget && this.target != null
                    && !this.target.isNull() && candidate.S() == this.target.S();
            Color markColor = attackedTarget
                    ? this.attackColor.getMutableColor() : this.targetColor.getMutableColor();
            if (this.ringMode.isSelected()) {
                float entityWidth = candidate.isInstance(MappedClasses.Yl) || candidate.isInstance(MappedClasses.lG)
                        ? 0.7f : candidate.f$src$F$fst3ac();
                JelloTargetMarkRenderer.draw(candidate.c(), candidate.A(), candidate.Z(),
                        entityWidth, candidate.Y(), markColor,
                        this.jelloWidth.getValue().floatValue(),
                        this.jelloThickness.getValue().floatValue(),
                        this.jelloHeight.getValue().floatValue(),
                        this.jelloOffset.getValue().floatValue(),
                        this.jelloSpeed.getValue().longValue(),
                        this.jelloOpacity.getValue().floatValue() / 100.0f);
            } else {
                RenderUtil.k(candidate, 1.0, null, markColor, eventRender3D.getTicks());
            }
        }
    }

    private boolean passesItemFilter(EntityLivingBase target) {
        if (this.limitToItems.getEffectiveValue().booleanValue()) {
            ItemStack itemStack = Minecraft.thePlayer().getHeldItemHand();
            if (!this.allowedItems.isValid(itemStack, false)) {
                return false;
            }
            return this.targetFilter.isValidTarget(target);
        }
        return this.targetFilter.isValidTarget(target);
    }

    @Override
    public String getDetailedSuffix() {
        if (ForgeVersion.MC_1_12_2.d() && this.perfectSwing.getEffectiveValue().booleanValue()) {
            float attackStrength = Minecraft.thePlayer().getCooledAttackStrength(0.0f);
            if (attackStrength == 1.0f) {
                return "\u00a76Ready";
            }
            return String.format("%.1f", Float.valueOf(attackStrength));
        }
        return this.switchTargets.getEffectiveValue().booleanValue() ? "Switch" : "Single";
    }

    public boolean isValidTarget(EntityLivingBase candidate) {
        if (candidate == null || candidate.isNull()) {
            return false;
        }
        if (candidate.equals(Minecraft.thePlayer())) {
            return false;
        }
        // Vanished and invisible players are still in the world entity list but
        // cannot be hit, so aiming at them only pulls the aura away from a real
        // target. Target rejects them for the same reason.
        try {
            if (candidate.isDead() || candidate.isInvisible()) {
                return false;
            }
        } catch (Throwable ignored) {
            // Missing mappings must not make every target invalid.
        }
        // Server bots often exist as normal, visible players without an entry
        // in the tab list.  PlayerVisibility rejects those for safety, but Aura
        // may explicitly acquire them through this opt-in setting.  Invisible
        // entities remain rejected by the check immediately above.
        if (!PlayerVisibility.isInteractable(candidate)
                && !this.attackBots.getEffectiveValue().booleanValue()) {
            return false;
        }
        if (candidate.w$src$F$15l9epb() <= 0.0f || candidate.M$src$Z$ff28xj()) {
            return false;
        }
        if (!this.isWithinAcquisitionRange(candidate)) {
            return false;
        }
        if (RotationUtil.a(Minecraft.thePlayer(), candidate) > ((Double)this.maxAngle.getValue()).intValue() / 2) {
            return false;
        }
        if (SonyVegas.INSTANCE.getFriendManager().isFriend(candidate)) {
            return false;
        }
        // Clan membership is a visual/Target-module concern, not an attack
        // permission. Aura considers every valid player and only excludes
        // friends through the normal friend and target filters.
        if (candidate.equals(Minecraft.thePlayer().S$src$Lvegas_sony_wrapper_impl_Entity_$dgzs12())) {
            return false;
        }
        return this.passesItemFilter(candidate);
    }

    private boolean isWithinAcquisitionRange(EntityLivingBase target) {
        return this.isWithinRange(target, getSwingRange(this.extraSwingDistance.getValue()))
                && !this.isIgnoredInWeb(target);
    }

    private boolean isIgnoredInWeb(EntityLivingBase candidate) {
        return this.ignoreInWeb.getEffectiveValue().booleanValue()
                && EntityWebDetection.isInWeb(candidate);
    }

    static double getSwingRange(double extraDistance) {
        return ACQUISITION_REACH + Math.max(0.0, Math.min(3.0, extraDistance));
    }

    static boolean isWithinConfiguredRange(double distance, double reach) {
        return !Double.isNaN(distance) && !Double.isInfinite(distance)
                && distance >= 0.0 && distance <= reach;
    }

    private boolean isWithinAttackRange(EntityLivingBase target) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || target == null || target.isNull()
                || this.isIgnoredInWeb(target)) {
            return false;
        }
        // Do not round the attack distance to float. At the packet boundary,
        // 3.0000001 must stay outside the configured 3.000000 block reach.
        double dx = player.z() - target.z();
        double dy = player.N() - target.N();
        double dz = player.h() - target.h();
        return isWithinConfiguredRange(Math.sqrt(dx * dx + dy * dy + dz * dz),
                this.getAttackRange());
    }

    private boolean isWithinRange(EntityLivingBase target, double reach) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || target == null || target.isNull()) {
            return false;
        }
        // Use the same centre-to-centre distance players and server-side
        // reach checks report.  A nearest-hitbox calculation lets a target's
        // width hide several tenths of a block and was the source of hits sent
        // after the displayed distance had already passed 3.0.
        double distance = player.getDistanceToEntity(target);
        return isWithinConfiguredRange(distance, reach);
    }

    private void randomizeGainFactors() {
        this.pitchProportionalScale = 1.0f;
        this.pitchIntegralScale = 1.0f;
        this.yawProportionalScale = 1.0f;
        this.yawIntegralScale = 1.0f;
    }

    public boolean isAttackCooldownReady() {
        if (ForgeVersion.MC_1_12_2.d() && this.perfectSwing.getEffectiveValue().booleanValue()) {
            float attackStrength = Minecraft.thePlayer().getCooledAttackStrength(0.0f);
            return attackStrength == 1.0f;
        }
        return this.attackTiming.isReady(System.nanoTime());
    }

    private boolean handleBreakBlocks() {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return false;
        }
        if (this.breakBlocks.getEffectiveValue().booleanValue() && this.breakBlocksTimer.hasTimeElapsed(this.nextBreakBlockTime)) {
            if (this.breakBlocksWhitelist.getEffectiveValue().booleanValue() && !this.blockBreakItems.matches(player.getHeldItemHand())) {
                return false;
            }
            if (Minecraft.currentScreen().isInstance(MappedClasses.Ft)) {
                return false;
            }
            RayTraceResult rayTraceResult = RayTraceUtil.o();
            if (rayTraceResult.isNotNull() && rayTraceResult.isBlockHit() && ClientSettings.isAttackButtonDown()) {
                KeyBinding keyBinding = Minecraft.gameSettings().F();
                KeyBindingHelper.updateKeyBinding(keyBinding, true, false);
                return true;
            }
            this.nextBreakBlockTime = (long)this.breakBlocksDelay.getRandomValue();
            this.breakBlocksTimer.reset();
        }
        return false;
    }

    public SilentAura() {
        super("Aura //$ui", (int)MODULE_ID, Category.GLADIADOR, "Simulates feel of Killaura\nAttacks and aims safely using built in AutoClicker to click, and Silent Aim system to aim //$ui");
        this.requireMouseDown = BooleanValue.create(this, "Require mouse down //$ui", false);
        this.disableOnDeath = BooleanValue.create(this, "Disable on death //$ui", false);
        this.showTarget = BooleanValue.create(this, "Show target //$ui", false);
        this.switchTargets = BooleanValue.create(this, "Switch //$ui", false, "Attacks other targets while current target is in hit cooldown //$ui");
        this.limitToItems = BooleanValue.create(this, "Limit to items //$ui", false, "Aura functions only while holding selected items //$ui");
        this.breakBlocks = BooleanValue.create(this, "Break blocks //$ui", false, "Prevents from aiming while attempting to break blocks //$ui");
        this.breakBlocksDelay = RandomValue.createWithDescription(this, "Break blocks delay //$ui", "#", "", 0.0, 0.0, 10.0, 2000.0, 1.0, "Delay in milliseconds before breaking blocks //$ui");
        this.breakBlocksWhitelist = BooleanValue.create(this, "Break blocks whitelist //$ui", false);
        this.blockBreakItems = LimitValue.create(this, "SilentBlockBreakingItems //$", "Block breaking items //$ui", LimitValue.ALLOW_LIST_COLOR, Arrays.asList(new ItemLimitData("pickaxes //$"), new ItemLimitData("shovels //$")));
        this.attackRate = RandomClickDelayValue.create(this, "Attacks per Second //$ui", "#.#", "", 1.0, 6.0, 13.0, 20.0);
        this.attackRate.addChangeListener(value -> this.attackTiming.reset());
        this.legitHitMode = new ModeOption("Legit //$ui");
        this.blatantHitMode = new ModeOption("Blatant //$ui");
        // Keep the current attack timing as the default for existing profiles.
        // Legit remains available when the player wants every hit to wait for
        // the completed ray trace instead of the aura's near-target threshold.
        this.hitMode = ModeValue.create(this, "Hit Mode //$ui", this.blatantHitMode,
                this.legitHitMode, this.blatantHitMode);
        this.failSwing = BooleanValue.create(this, "Fail swing //$ui", true,
                "While pulling aim, swing like Legit; when the aim reaches the switch target hitbox, hold the swing and send the packet //$ui");
        this.forceFirstHit = BooleanValue.create(this, "Force first hit //$ui", true,
                "First hit on a new switch target bypasses the CPS delay as soon as the aim is on the hitbox //$ui");
        this.maxAngle = NumberValue.create(this, "Max angle //$ui", "#", "", 1.0, 120.0, 180.0, 5.0, "Ângulo máximo para adquirir e mirar um alvo a partir do cursor //$ui");
        this.targetColor = ColorValue.createWithAlpha(this, "Target color //$ui", new Color(255, 200, 112), 50);
        this.attackColor = ColorValue.create(this, "Attack color //$ui", new Color(255, 0, 0, 100));
        this.distanceMode = new ModeOption("Distance //$ui");
        this.yawMode = new ModeOption("Yaw //$ui");
        this.armorMode = new ModeOption("Armor //$ui");
        this.threatMode = new ModeOption("Threat //$ui");
        this.healthMode = new ModeOption("Health //$ui");
        this.targetMode = ModeValue.create((Object)this, "Target mode //$ui", "How Aura should prioritize targets\nArmor/Threat will default to Distance for non player targets //$ui", (ModeSelection)this.distanceMode, this.distanceMode, this.yawMode, this.armorMode, this.threatMode, this.healthMode);
        this.switchDelay = NumberValue.create(this, "Switch delay //$ui", "#", "", 0.0, 100.0, 750.0, 25.0, "Minimum time the aura stays on a target after a hit, in milliseconds //$ui");
        this.allowedItems = LimitValue.create(this, "v-7d2a9c31 //$", "Allowed Items //$ui", LimitValue.ALLOW_LIST_COLOR, Collections.emptyList());
        this.ringMode = new ModeOption("Jello //$ui");
        this.boxMode = new ModeOption("Box //$ui");
        this.renderType = ModeValue.create((Object)this, "Render type //$ui", this.ringMode, this.ringMode, this.boxMode);
        this.jelloWidth = NumberValue.create(this, "Jello width //$ui", "#.##", "x",
                0.5, 1.6, 3.0, 0.05, "Controls the radius around each target //$ui");
        this.jelloThickness = NumberValue.create(this, "Jello thickness //$ui", "#.##", "x",
                0.25, 1.25, 3.0, 0.05, "Controls the trail and line thickness //$ui");
        this.jelloHeight = NumberValue.create(this, "Jello height //$ui", "#.##", "x",
                0.5, 1.38, 2.5, 0.05, "Controls how high the marker travels //$ui");
        this.jelloOffset = NumberValue.create(this, "Jello offset //$ui", "#.##", "",
                0.0, 0.72, 1.5, 0.05, "Moves the animation endpoints away from the entity base //$ui");
        this.jelloSpeed = NumberValue.create(this, "Jello speed //$ui", "#", "ms",
                250.0, 2000.0, 5000.0, 50.0, "Duration of one complete animation cycle //$ui");
        this.jelloOpacity = NumberValue.create(this, "Jello opacity //$ui", "#", "%",
                10.0, 78.0, 100.0, 1.0, "Maximum opacity of the Jello trail //$ui");
        this.perfectSwing = BooleanValue.create(this, "Perfect swing //$ui", false, "Only attacks when there is no attack cooldown\nAdditionally, only swings when hovering(trigger) //$ui");
        this.breakBlocksTimer = new TimerUtil();
        this.rotationClaim = SharedModuleControlClaims.rotation;
        this.perfectSwing.whenEqualTo(false).applyTo(this.attackRate);
        this.addValue(this.targetFilter, this.attackBots, this.ignoreInWeb, this.aimSpeed, this.attackRate, this.hitMode,
                this.failSwing, this.forceFirstHit,
                this.extraSwingDistance, this.maxAngle, this.targetMode);
        this.hitMode.addModeDependentValues(this.blatantHitMode, this.failSwing, this.forceFirstHit);
        this.showTarget.addDependentValues(this.syncMarkWithHit, this.targetColor, this.attackColor, this.renderType,
                this.jelloWidth, this.jelloThickness, this.jelloHeight,
                this.jelloOffset, this.jelloSpeed, this.jelloOpacity);
        this.renderType.addModeDependentValues(this.ringMode,
                this.jelloWidth, this.jelloThickness, this.jelloHeight,
                this.jelloOffset, this.jelloSpeed, this.jelloOpacity);
        this.breakBlocks.addDependentValues(this.breakBlocksDelay, this.breakBlocksWhitelist);
        this.breakBlocksWhitelist.addDependentValues(this.blockBreakItems);
        this.switchTargets.addDependentValues(this.switchDelay);
        this.addValue(this.switchDelay);
        this.U(this.perfectSwing, ForgeVersion.MC_1_8_9.N());
        this.U(this.switchTargets, ForgeVersion.MC_1_8_9.H());
        this.addValue(new Value[]{this.disableOnDeath, this.breakBlocks, this.breakBlocksDelay, this.breakBlocksWhitelist, this.blockBreakItems, this.requireMouseDown,
                this.showTarget, this.syncMarkWithHit, this.targetColor, this.attackColor, this.renderType,
                this.jelloWidth, this.jelloThickness, this.jelloHeight,
                this.jelloOffset, this.jelloSpeed, this.jelloOpacity,
                this.limitToItems.addDependentValues(this.allowedItems), this.allowedItems});
        this.limitToItems.setCompactListValue(this.allowedItems);
        this.rotationClaim.setPriority(this, 5);
        this.attackRate.setMaximumFractionDigits(0);
    }

    static {
        MODULE_ID = 267655872188715318L;
    }

    public boolean canAttack() {
        if (Minecraft.theWorld().isNull()) {
            return false;
        }
        if (!this.isAttackCooldownReady()) {
            return false;
        }
        if (!this.isHitModeReady()) {
            return false;
        }
        if (Minecraft.currentScreen().isNotNull()) {
            return false;
        }
        EntityLivingBase attackTarget = this.target;
        return !this.breakingBlocks && attackTarget != null
                && !attackTarget.isNull() && this.isWithinAttackRange(attackTarget);
    }

    private boolean isHitModeReady() {
        if (this.hitMode.getValue() == this.legitHitMode) {
            return this.isLookingAtTarget();
        }
        // Blatant: Fail swing começa a puxar como o Legit (apenas visual) e
        // segura até a mira chegar na hitbox do alvo do switch; o CPS só conta
        // a partir do hold, e o primeiro hit pode furar o delay (Force first hit).
        return this.readyToAttack || this.failSwingTargetId != Integer.MIN_VALUE;
    }

    private boolean isFailSwingTarget(EntityLivingBase candidate) {
        return candidate != null && !candidate.isNull()
                && this.failSwingTargetId != Integer.MIN_VALUE
                && candidate.S() == this.failSwingTargetId;
    }

    private void updateBlatantSwingState() {
        this.failSwingTargetId = Integer.MIN_VALUE;
        if (!this.blatantHitMode.isSelected()
                || !this.failSwing.getEffectiveValue().booleanValue()) {
            return;
        }
        EntityLivingBase swingTarget = this.target;
        if (swingTarget == null || swingTarget.isNull()
                || !this.isControllingRotation() || !this.isWithinAcquisitionRange(swingTarget)) return;
        // Igual ao Legit: só balança se o trace da rotação gerenciada chega na
        // hitbox. Enquanto está puxando, é só swing visual (sem packet).
        double swingRange = getSwingRange(this.extraSwingDistance.getValue());
        RayTraceResult trace = RotationManager.INSTANCE.rayTraceUsingManagedRotation(
                swingRange, 0.0f, swingRange > ACQUISITION_REACH);
        if (trace != null && trace.isNotNull() && trace.getEntity().isNotNull()
                && trace.getEntity().S() == swingTarget.S()
                && this.isWithinAttackRange(swingTarget)) {
            this.failSwingTargetId = swingTarget.S();
        }
    }

    @EventHandler
    public void onTick(EventPrePlayerTick eventPrePlayerTick) {
        // EventPreTick selects the target and prepares the silent rotation. Send
        // the direct attack here, after RotationManager has advanced that rotation
        // for this game tick, so a newly valid aim does not wait another tick.
        if (this.blatantHitMode.isSelected()) {
            this.updateBlatantSwingState();
            this.attackBlatantlyIfReady();
        } else {
            this.updateLegitSwingTarget();
        }
    }

    public boolean canSwingLegitVisual() {
        return !this.attackClickSuppressed
                && (this.canSwingLegit() || this.canSwingFailBlatant());
    }

    public boolean canClickAttack() {
        // Durante a janela do packet nenhum clique (fisico ou sintetico) pode
        // disparar: era isso que fazia o vanilla mandar digging junto do attack.
        if (this.attackClickSuppressed) {
            return false;
        }
        if (this.blatantHitMode.isSelected()) {
            // Blatant envia o proprio packet. O clicker nunca pode injetar um
            // clique aqui, senao o vanilla manda um segundo ataque e ainda
            // inicia digging quando a mira cai num bloco. O Fail swing e swing
            // visual puro (swingItem), sem mouse sintetico.
            return false;
        }
        if (ForgeVersion.MC_1_12_2.d() && this.perfectSwing.getEffectiveValue().booleanValue()) {
            return false;
        }
        // The worker already spaces Legit clicks at the configured CPS.
        // Waiting again from the eventual packet doubles that interval.
        return this.canSwingLegit();
    }

    private boolean canSwingLegit() {
        EntityLivingBase swingTarget = this.target;
        return this.isEnabled() && !this.breakingBlocks
                && Minecraft.currentScreen().isNull()
                && swingTarget != null && !swingTarget.isNull()
                && this.isControllingRotation()
                && this.legitSwingTargetId == swingTarget.S()
                && this.isWithinAcquisitionRange(swingTarget);
    }

    private boolean canSwingFailBlatant() {
        EntityLivingBase swingTarget = this.target;
        return this.isEnabled() && !this.breakingBlocks
                && Minecraft.currentScreen().isNull()
                && swingTarget != null && !swingTarget.isNull()
                && this.isControllingRotation()
                && this.isFailSwingTarget(swingTarget)
                && this.isWithinAcquisitionRange(swingTarget);
    }

    private void updateLegitSwingTarget() {
        this.legitSwingTargetId = Integer.MIN_VALUE;
        EntityLivingBase swingTarget = this.target;
        if (swingTarget == null || swingTarget.isNull()
                || !this.isControllingRotation() || !this.isWithinAcquisitionRange(swingTarget)) return;
        double swingRange = getSwingRange(this.extraSwingDistance.getValue());
        // Keep the normal nearest-entity/block trace in Legit. Extra Swing
        // must not let it click through an intervening player or wall.
        RayTraceResult trace = RotationManager.INSTANCE.rayTraceUsingManagedRotation(
                swingRange, 0.0f, swingRange > ACQUISITION_REACH);
        if (trace != null && trace.isNotNull() && trace.getEntity().isNotNull()
                && trace.getEntity().S() == swingTarget.S()) {
            this.legitSwingTargetId = swingTarget.S();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onSyntheticAttackRequest(SyntheticAttackRequestEvent event) {
        if (this.attackClickSuppressed) {
            event.setCancelled(true);
            return;
        }
        if (event.getSource() != this && event.getSource() != clicker) {
            return;
        }
        if (!this.blatantHitMode.isSelected()) {
            if (!this.canSwingLegit()) {
                event.setCancelled(true);
                return;
            }
            if (!this.isWithinAttackRange(this.target)) {
                event.setCancelled(true);
                return;
            }
        } else if (this.failSwing.getEffectiveValue().booleanValue()) {
            // Blatant com Fail swing: começo puxando igual ao Legit (só swing
            // visual) e só armo/envio o packet quando a mira chegou na hitbox
            // do alvo do switch.
            if (!this.canSwingFailBlatant() || !this.isWithinAttackRange(this.target)) {
                event.setCancelled(true);
                return;
            }
        }
        if (!this.armAuraAttack(this.target)) {
            this.readyToAttack = false;
            event.setCancelled(true);
        }
    }

    private void updateRotationColors() {
        if (this.target != null && this.isControllingRotation()) {
            this.onTarget = this.blatantHitMode.isSelected()
                    ? this.isLookingAtTargetIgnoringOtherEntities()
                    : this.isLookingAtTarget();
            this.rotationController.setSecondaryColor(this.attackColor.getMutableColor());
            this.rotationController.setPrimaryColor(this.targetColor.getMutableColor());
            this.rotationController.setUseSecondaryColor(this.onTarget);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onTick(EventPreTick eventPreTick) {
        // Resume held input at the next input tick, without generating a click.
        this.restoreClicksAfterAttackPacket();
        this.breakingBlocks = this.handleBreakBlocks();
        if (this.pendingAuraAttackTarget != null
                && System.currentTimeMillis() > this.pendingAuraAttackExpiresAt) {
            this.clearPendingAuraAttack();
        }
        if (this.perfectSwingAttackPending) {
            this.perfectSwingAttackPending = false;
            AttackKeyController.releaseAttackKey();
        }
        if (Minecraft.thePlayer().isNull()) {
            this.resetTargeting();
            return;
        }
        if (!this.isEnabled() || this.breakingBlocks) {
            this.resetTargeting();
            return;
        }
        if (clicker == null) {
            clicker = SonyVegas.INSTANCE.getModManager().getMod(SilentAuraClicker.class);
        }
        if (!clicker.isEnabled()) {
            clicker.setEnabled(true);
        }
        this.updateRotationColors();
        if (this.disableOnDeath.getEffectiveValue().booleanValue()) {
            if (Minecraft.thePlayer().M$src$Z$ff28xj() || Minecraft.thePlayer().w$src$F$15l9epb() <= 0.0f) {
                this.toggle();
                return;
            }
            if (ForgeVersion.MC_1_16_5.d()) {
                GuiScreen guiScreen = Minecraft.currentScreen();
                if (guiScreen.isNotNull()) {
                    if (!this.deathHandled && guiScreen.isInstance(MappedClasses.D2)) {
                        this.deathHandled = true;
                        this.toggle();
                        return;
                    }
                    this.deathHandled = false;
                }
            } else {
                TitledScreen titledScreen = Minecraft.k();
                if (titledScreen.isNotNull()) {
                    String screenTitle = titledScreen.getDisplayedTitle();
                    if (!this.deathHandled && screenTitle != null && (screenTitle.toLowerCase().contains("died") || screenTitle.toLowerCase().contains("dead"))) {
                        this.deathHandled = true;
                        this.toggle();
                        return;
                    }
                    if (screenTitle == null || screenTitle.equals("")) {
                        this.deathHandled = false;
                    }
                }
            }
        }
        if (Minecraft.currentScreen().isNotNull()
                || !InputEventDispatcher.getInstance().getFocusState().isFocused()
                || this.requireMouseDown.getEffectiveValue().booleanValue() && !ClientSettings.isAttackButtonDown()) {
            // Chat, inventory or an unfocused window: stop aiming and clicking
            // instead of quietly holding the target.
            this.resetTargeting();
            return;
        }
        this.updateTarget();
        this.updateAim();
        if (!this.blatantHitMode.isSelected()
                && ForgeVersion.MC_1_12_2.d()
                && this.perfectSwing.getEffectiveValue().booleanValue()
                && this.isAttackCooldownReady() && this.canSwingLegit()) {
            AttackKeyController.releaseAttackKey();
            this.perfectSwingAttackPending = AttackKeyController.requestSyntheticAttack(this);
        }
    }

    private void attackBlatantlyIfReady() {
        if (!this.isEnabled() || Minecraft.theWorld().isNull()) {
            return;
        }
        boolean failSwingOn = this.blatantHitMode.isSelected()
                && this.failSwing.getEffectiveValue().booleanValue();
        if (!failSwingOn) {
            if (!this.canAttack() || !this.isLookingAtTargetIgnoringOtherEntities()) {
                return;
            }
        } else {
            // Fail swing: enquanto puxa a mira, envia swing igual ao Legit
            // (visual, sem packet). Só quando chegar na hitbox do alvo do
            // switch é que segura e envia o packet — respeitando o CPS, com o
            // primeiro hit podendo furar o delay (Force first hit).
            if (Minecraft.currentScreen().isNotNull() || this.breakingBlocks) {
                return;
            }
            if (!this.canSwingFailBlatant() || !this.isLookingAtTargetIgnoringOtherEntities()) {
                this.swingFailBlatantVisual();
                return;
            }
            if (!this.isAttackCooldownReady()) {
                return;
            }
            if (!this.consumeBlatantClickSlot()) {
                return;
            }
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        EntityLivingBase attackTarget = this.target;
        if (player.isNull() || attackTarget == null || attackTarget.isNull()) {
            return;
        }
        if (!this.armAuraAttack(attackTarget)) {
            this.readyToAttack = false;
            return;
        }
        if (!this.suppressClicksForAttackPacket()) {
            this.clearPendingAuraAttack();
            return;
        }
        if (ForgeVersion.MC_1_12_2.v()) {
            player.m$src$V$15frh5h();
            Minecraft.playerController().attackEntity(player, attackTarget);
        } else {
            Minecraft.playerController().attackEntity(player, attackTarget);
            player.m$src$V$15frh5h();
        }
        this.firstHitPending = false;
    }

    private void swingFailBlatantVisual() {
        if (this.attackClickSuppressed || !this.blatantHitMode.isSelected()
                || !this.failSwing.getEffectiveValue().booleanValue()) {
            return;
        }
        EntityLivingBase swingTarget = this.target;
        if (swingTarget == null || swingTarget.isNull()
                || !this.isControllingRotation() || !this.isWithinAcquisitionRange(swingTarget)) {
            return;
        }
        try {
            Minecraft.thePlayer().m$src$V$15frh5h();
        } catch (Throwable ignored) {
        }
    }

    /** End the existing vanilla mining action before taking over for an attack. */
    private boolean suppressClicksForAttackPacket() {
        if (this.attackClickSuppressed) {
            return false;
        }
        this.attackClickSuppressed = true;
        try {
            KeyBinding attackKey = Minecraft.gameSettings().F();
            attackKey.e();
            this.endingBlockBreaking = true;
            try {
                Minecraft.stopBlockBreaking();
            } finally {
                this.endingBlockBreaking = false;
            }
            return !Minecraft.playerController().a();
        } catch (RuntimeException | LinkageError error) {
            SonyVegas.logThrowable(error);
            return false;
        }
    }

    /** Reconcile with current input, including a button released during the pause. */
    private void restoreClicksAfterAttackPacket() {
        if (!this.attackClickSuppressed) {
            return;
        }
        try {
            KeyBinding attackKey = Minecraft.gameSettings().F();
            KeyBindingHelper.updateKeyBinding(attackKey,
                    ClientSettings.isPhysicalKeyDown(attackKey), false);
        } catch (RuntimeException | LinkageError error) {
            SonyVegas.logThrowable(error);
        } finally {
            this.attackClickSuppressed = false;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onClickMouse(EventClickMouse event) {
        if (this.attackClickSuppressed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onSendClickBlockToController(EventSendClickBlockToController eventSendClickBlockToController) {
        // The explicit released-input call must reach vanilla to stop mining;
        // subsequent held-input calls stay paused until the next tick.
        if (this.attackClickSuppressed && !this.endingBlockBreaking) {
            eventSendClickBlockToController.setCancelled(true);
        }
    }

    private boolean consumeBlatantClickSlot() {
        boolean firstHit = this.forceFirstHit.getEffectiveValue().booleanValue()
                && this.firstHitPending;
        if (firstHit) {
            return true;
        }
        // Respeita o CPS configurado: usa o mesmo timer do clicker/worker
        // (hasClickDelayElapsed sem arg consome o delay calculado do CPS), sem
        // criar um segundo relógio que dobraria o intervalo.
        if (!this.attackTiming.isReady(System.nanoTime())) {
            return false;
        }
        try {
            if (!this.attackRate.hasClickDelayElapsed()) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPacketSend(EventPacketSend event) {
        Packet packet = event.getPacket();
        if (!UseEntityPacketBridge.isUseEntityPacket(packet)) {
            return;
        }
        UseEntityPacketBridge attackPacket = new UseEntityPacketBridge(packet);
        if (!attackPacket.isAttack()) {
            return;
        }
        int entityId = attackPacket.getEntityId();
        EntityLivingBase armedTarget = this.getPendingAuraAttackTarget(entityId);
        EntityLivingBase currentTarget = this.target;
        EntityLivingBase attackTarget = armedTarget != null ? armedTarget
                : currentTarget != null && !currentTarget.isNull()
                        && currentTarget.S() == entityId ? currentTarget : null;
        if (attackTarget == null) {
            return;
        }
        // The click may have been requested while the target was close, then
        // placed in the input/network queue while either player moved. Always
        // check the captured target at the actual packet boundary.
        if (!this.isWithinAttackRange(attackTarget)) {
            this.readyToAttack = false;
            this.clearPendingAuraAttack();
            event.setCancelled(true);
            return;
        }
        // Do not turn a late packet for an old switched target into a hit on
        // the new target's marker or click timer.
        if (currentTarget != null && !currentTarget.isNull()
                && currentTarget.S() == entityId) {
            this.recordTargetHit();
            this.attackRate.resetClickTimer();
            this.attackTiming.recordAttack(System.nanoTime(), this.attackRate.getRandomValue());
        }
    }

    private boolean armAuraAttack(EntityLivingBase candidate) {
        if (!this.isWithinAttackRange(candidate)) {
            return false;
        }
        this.pendingAuraAttackTarget = candidate;
        this.pendingAuraAttackExpiresAt = System.currentTimeMillis()
                + PENDING_AURA_ATTACK_TIMEOUT_MS;
        return true;
    }

    private EntityLivingBase getPendingAuraAttackTarget(int entityId) {
        EntityLivingBase candidate = this.pendingAuraAttackTarget;
        if (candidate == null || System.currentTimeMillis() > this.pendingAuraAttackExpiresAt) {
            this.clearPendingAuraAttack();
            return null;
        }
        if (candidate.isNull() || candidate.S() != entityId) {
            return null;
        }
        return candidate;
    }

    private void clearPendingAuraAttack() {
        this.pendingAuraAttackTarget = null;
        this.pendingAuraAttackExpiresAt = 0L;
    }

    /**
     * Called by a guarded packet queue immediately before it writes an attack
     * to the network. Guarded dispatches bypass the usual packet event, so the
     * normal onPacketSend check alone cannot protect a delayed Aura click.
     */
    public boolean mayDispatchDeferredAttack(Packet packet) {
        if (packet == null || packet.isNull()
                || !UseEntityPacketBridge.isUseEntityPacket(packet)) {
            return true;
        }
        UseEntityPacketBridge attack = new UseEntityPacketBridge(packet);
        if (!attack.isAttack()) {
            return true;
        }
        int entityId = attack.getEntityId();
        EntityLivingBase armedTarget = this.getPendingAuraAttackTarget(entityId);
        EntityLivingBase currentTarget = this.target;
        EntityLivingBase attackTarget = armedTarget != null ? armedTarget
                : currentTarget != null && !currentTarget.isNull()
                        && currentTarget.S() == entityId ? currentTarget : null;
        if (attackTarget == null) {
            return true;
        }
        if (this.isWithinAttackRange(attackTarget)) {
            return true;
        }
        this.readyToAttack = false;
        this.clearPendingAuraAttack();
        return false;
    }

    private void recordTargetHit() {
        this.hitCurrentTarget = true;
        if (!this.switchTargets.getEffectiveValue().booleanValue() || this.target == null) {
            this.clearPendingSwitch();
            return;
        }
        int targetId = this.target.S();
        if (this.pendingSwitchTargetId != targetId) {
            this.pendingSwitchTargetId = targetId;
            this.pendingSwitchAt = System.currentTimeMillis() + this.getEffectiveSwitchDelay();
        }
    }

    private void applyPendingSwitch() {
        if (this.pendingSwitchTargetId == Integer.MIN_VALUE || System.currentTimeMillis() < this.pendingSwitchAt) {
            return;
        }
        if (!this.prevTargetIds.contains(this.pendingSwitchTargetId)) {
            this.prevTargetIds.add(this.pendingSwitchTargetId);
        }
        this.clearPendingSwitch();
    }

    private long getEffectiveSwitchDelay() {
        double configured = this.switchDelay.getValue();
        if (configured < this.switchDelay.getMinimum() || configured > this.switchDelay.getMaximum()) {
            configured = 100.0;
        }
        return Math.max(0L, Math.round(configured));
    }

    private void clearPendingSwitch() {
        this.pendingSwitchTargetId = Integer.MIN_VALUE;
        this.pendingSwitchAt = 0L;
    }

    static boolean isSwitchDelayHolding(boolean switchEnabled, int pendingTargetId,
                                        int currentTargetId, long switchAt, long now) {
        return switchEnabled && pendingTargetId != Integer.MIN_VALUE
                && pendingTargetId == currentTargetId && now < switchAt;
    }

    private boolean isHoldingSwitchTarget() {
        return this.target != null && !this.target.isNull()
                && !this.isIgnoredInWeb(this.target)
                && isSwitchDelayHolding(this.switchTargets.getEffectiveValue(),
                        this.pendingSwitchTargetId, this.target.S(),
                        this.pendingSwitchAt, System.currentTimeMillis());
    }

    static <T> T chooseDuringSwitchDelay(ArrayList<T> candidates, T currentTarget,
                                         boolean holdCurrentTarget) {
        return holdCurrentTarget ? currentTarget
                : candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean isControllingRotation() {
        MouseRotationController mouseRotationController = RotationManager.INSTANCE.getActiveController();
        return mouseRotationController != null && mouseRotationController.equals(this.rotationController);
    }

    private void updateTarget() {
        ArrayList<EntityLivingBase> candidates = new ArrayList<EntityLivingBase>();
        if (this.shouldSkip()) {
            this.renderTargets.clear();
            return;
        }
        this.refreshRenderTargets();
        Target targetModule = SonyVegas.INSTANCE.getModManager().getMod(Target.class);
        if (targetModule != null && targetModule.isAwaitingBrowseSelection()) {
            this.resetTargeting();
            return;
        }
        EntityLivingBase selectedByTarget = targetModule != null && targetModule.isEnabled()
                ? targetModule.getCurrentTarget() : null;
        boolean targetModuleControlsSelection = selectedByTarget != null && this.isValidTarget(selectedByTarget);
        if (targetModule != null && targetModule.isInvseeSelectionRequired() && !targetModuleControlsSelection) {
            this.resetTargeting();
            return;
        }
        if (!this.switchTargets.getEffectiveValue().booleanValue() || targetModuleControlsSelection) {
            this.clearPendingSwitch();
            this.prevTargetIds.clear();
        } else {
            this.applyPendingSwitch();
        }
        // The delay is a real target lock after a sent hit, not merely a delay
        // before adding the old ID to the exclusion list. Keep the same entity
        // even when sorting changes or it leaves the acquisition range.
        boolean holdCurrentTarget = !targetModuleControlsSelection && this.isHoldingSwitchTarget();
        if (targetModuleControlsSelection) {
            candidates.add(selectedByTarget);
        } else {
            candidates.addAll(this.renderTargets);
        }
        if (!holdCurrentTarget) {
            if (!targetModuleControlsSelection && this.targetMode.getValue() == this.yawMode) {
                candidates.sort(new EntityAngleComparator());
            } else if (this.targetMode.getValue() == this.distanceMode) {
                candidates.sort(new EntityDistanceComparator());
            } else if (this.targetMode.getValue() == this.threatMode) {
                candidates.sort(new EntityArmorValueComparator());
            } else if (this.targetMode.getValue() == this.armorMode) {
                candidates.sort(new EntityEquipmentValueComparator());
            } else if (this.targetMode.getValue() == this.healthMode) {
                candidates.sort(new EntityHealthComparator());
            }
        }
        if (!holdCurrentTarget && this.switchTargets.getEffectiveValue().booleanValue()
                && !targetModuleControlsSelection && !candidates.isEmpty()) {
            ArrayList<EntityLivingBase> filtered = new ArrayList<EntityLivingBase>();
            for (EntityLivingBase entity : candidates) {
                if (!this.prevTargetIds.contains(entity.S())) {
                    filtered.add(entity);
                }
            }
            if (!filtered.isEmpty()) {
                candidates = filtered;
            } else if (!this.prevTargetIds.isEmpty()) {
                this.prevTargetIds.clear();
            }
        } else if (!this.switchTargets.getEffectiveValue().booleanValue() && !this.prevTargetIds.isEmpty()) {
            this.prevTargetIds.clear();
        }
        EntityLivingBase selectedTarget = chooseDuringSwitchDelay(candidates, this.target, holdCurrentTarget);
        if (selectedTarget != null) {
            boolean targetChanged = this.target != null && !this.target.equals(selectedTarget);
            if (this.target == null || targetChanged) {
                this.legitSwingTargetId = Integer.MIN_VALUE;
                this.failSwingTargetId = Integer.MIN_VALUE;
                this.firstHitPending = true;
                this.hitCurrentTarget = false;
                AttackKeyController.releaseAttackKey();
                this.randomizeGainFactors();
            }
            this.target = selectedTarget;
            if (!this.rotationClaim.isOwnedBy(this)) {
                this.rotationClaim.acquire(this, true);
            }
            this.readyToAttack = false;
            if (this.rotationController != null) {
                if (this.blatantHitMode.isSelected()) {
                    this.readyToAttack = this.isWithinAttackRange(this.target);
                } else if (ForgeVersion.MC_1_12_2.d() && this.perfectSwing.getEffectiveValue().booleanValue()) {
                    RayTraceResult rayTrace = RotationManager.INSTANCE.getExtendedReachRayTrace();
                    if (rayTrace.isNotNull() && rayTrace.getEntity().isNotNull() && rayTrace.getEntity().equals(this.target)) {
                        this.readyToAttack = true;
                    }
                } else {
                    double angularDistance = RotationUtil.L(this.target);
                    if (angularDistance < 3.0 && this.isWithinAttackRange(this.target)) {
                        this.readyToAttack = true;
                    }
                }
            }
        } else {
            this.resetTargeting();
        }
    }

    private void refreshRenderTargets() {
        this.renderTargets.clear();
        if (Minecraft.theWorld().isNull() || Minecraft.thePlayer().isNull()) return;
        ArrayList<?> loadedEntities = new ArrayList<Object>(Minecraft.theWorld().z());
        for (Object entityObject : loadedEntities) {
            Entity entity = new Entity(entityObject);
            EntityLivingBase candidate;
            if (ClientSettings.IS_LEGACY_1_7 && entity.isInstance(MappedClasses.FT)
                    || !entity.isInstance(MappedClasses.zm)
                    || !this.isValidTarget(candidate = new EntityLivingBase(entityObject))) {
                continue;
            }
            this.renderTargets.add(candidate);
        }
    }

    private Map<String, Integer> getScoreboardHealthMap() {
        try {
            Scoreboard scoreboard = Minecraft.theWorld().getScoreboard();
            if (scoreboard == null) {
                return Collections.emptyMap();
            }
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
            if (objective == null) {
                return Collections.emptyMap();
            }
            Collection<Score> scores = scoreboard.getPlayerScores(objective);
            HashMap<String, Integer> healthByPlayer = new HashMap<String, Integer>();
            for (Score score : scores) {
                String owner = score.getOwner();
                if (owner != null && !healthByPlayer.containsKey(owner)) {
                    healthByPlayer.put(owner, score.getScore());
                }
            }
            return healthByPlayer;
        }
        catch (Throwable throwable) {
            return Collections.emptyMap();
        }
    }

    private double[] computeAimCoords(EntityLivingBase target) {
        EntityPlayerSP player = Minecraft.thePlayer();
        AxisAlignedBB box = target.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl();

        // Preserve the user's vertical crosshair intent.  Project the current
        // view pitch to the target's horizontal distance, then clamp it to the
        // hit box.  Unlike a nearest-point/chest target this neither travels
        // from feet to head nor forces every opponent to the same height.
        // Keep the eye position on the stable position/rotation accessors used
        // by the old Aura path; getPositionEyes is not available in every
        // runtime mapping profile and can abort the rotation update entirely.
        double eyesX = player.z();
        double eyesY = player.N() + 1.62;
        double eyesZ = player.h();
        double dx = target.z() - eyesX;
        double dz = target.h() - eyesZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double lookedY = eyesY - Math.tan(Math.toRadians(player.V()))
                * horizontalDistance;
        double inset = Math.min(0.05, Math.max(0.0,
                (box.getMaxY() - box.getMinY()) * 0.25));
        double minY = box.getMinY() + inset;
        double maxY = box.getMaxY() - inset;
        double targetY = Math.max(minY, Math.min(maxY, lookedY));
        double heightRatio = target.Y() <= 0.0 ? 0.5
                : Math.max(0.0, Math.min(1.0, (targetY - box.getMinY()) / target.Y()));
        double previousTargetY = target.H() + target.Y() * heightRatio;
        return new double[]{target.z(), targetY, target.h(),
                target.f(), previousTargetY, target.R()};
    }

    private void updateAim() {
        if (Minecraft.theWorld().isNull()) {
            this.resetTargeting();
            return;
        }
        boolean hasValidTarget = this.target != null && this.isValidTarget(this.target);
        boolean ownsRotation = this.rotationClaim.isOwnedBy(this);
        if (hasValidTarget && ownsRotation) {
            this.resetJitter();
            EntityPlayerSP player = Minecraft.thePlayer();
            double[] aimCoordinates = this.computeAimCoords(this.target);
            double targetX = aimCoordinates[0];
            double targetY = aimCoordinates[1];
            double targetZ = aimCoordinates[2];
            double previousTargetX = aimCoordinates[3];
            double previousTargetZ = aimCoordinates[5];
            double distanceToTarget = player.i(targetX, targetY, targetZ);
            double targetMotionX = targetX - previousTargetX;
            double targetMotionZ = targetZ - previousTargetZ;
            double horizontalTargetMotion = Math.sqrt(targetMotionX * targetMotionX + targetMotionZ * targetMotionZ);
                boolean closeRange = distanceToTarget < 1.4;
                double jitteredTargetX = targetX;
                double jitteredTargetZ = targetZ;
            double jitteredTargetY = targetY;
            if (this.rotationController == null) {
                this.rotationController = new SilentAuraRotationController(this);
                this.rotationController.setRelativeMode(false);
                this.rotationController.setRetainAfterCompletion(true);
                this.rotationController.setClampStepToRemaining(true);
                this.rotationController.setTolerance(0.0f);
                this.rotationController.setScaleAxesProportionally(false);
                this.rotationController.setLinearAcceleration(false);
                RotationManager.INSTANCE.setController(this.rotationController);
            }
            // A newly created controller must receive its target in this same
            // tick.  Waiting for the next tick left a visible aim-only delay
            // before the first possible hit.
            this.rotationController.setRelativeMode(false);
            this.rotationController.setScaleAxesProportionally(false);
            this.rotationController.setLinearAcceleration(false);
            RotationAngles desiredRotation = this.rotationController.calculateRotation(Vec3.create(jitteredTargetX, jitteredTargetY, jitteredTargetZ));
            float targetPitch = (float)RotationUtil.h(player, targetX, jitteredTargetY, targetZ);
            float managedYaw = RotationManager.INSTANCE.getManagedYaw();
            float managedPitch = RotationManager.INSTANCE.getManagedPitch();
            float yawError = MathUtil.wrapAngleTo180(desiredRotation.getYaw() - managedYaw);
            float pitchError = MathUtil.wrapAngleTo180(targetPitch - managedPitch);
            if (closeRange) {
                // Vertical overlap is the case where pitch matters most.  The
                // old close-range branch erased it, so an opponent on top of
                // us could be pulled toward forever without ever being hit.
                this.pitchIntegral = 0.0f;
                this.yawIntegral = 0.0f;
                this.rotationController.setTargetRotation(desiredRotation.getYaw(), targetPitch);
            } else {
                float previousPitchStep = managedPitch - RotationManager.INSTANCE.getPreviousManagedPitch();
                float previousYawStep = managedYaw - RotationManager.INSTANCE.getPreviousManagedYaw();
                float integrationStep = 0.05f;
                boolean yawContinuingInSameDirection = Math.signum(yawError) == Math.signum(previousYawStep);
                double playerHorizontalSpeed = Math.sqrt(player.t() * player.t() + player.T() * player.T());
                float pitchProportionalGain = 0.45f * this.pitchProportionalScale;
                float pitchIntegralGain = 0.91f * this.pitchIntegralScale;
                float yawProportionalGain = (this.onTarget ? 0.05f : 0.1f) * this.yawProportionalScale;
                float yawIntegralGain = 0.33f * this.yawIntegralScale;
                if (yawContinuingInSameDirection && Math.abs(yawError) < 20.0f) {
                    yawProportionalGain *= 2.5f;
                    previousYawStep *= (float)(1.0 + Math.min(horizontalTargetMotion + playerHorizontalSpeed, 0.25));
                }
                float pitchControlError = pitchError - previousPitchStep;
                float yawControlError = yawError - previousYawStep;
                this.pitchIntegral += pitchControlError * integrationStep;
                this.yawIntegral += yawControlError * integrationStep;
                float pitchAdjustment = pitchProportionalGain * pitchControlError + pitchIntegralGain * this.pitchIntegral;
                float yawAdjustment = yawProportionalGain * yawControlError + yawIntegralGain * this.yawIntegral;
                if (Math.abs(yawError) > 120.0f) {
                    this.yawIntegral = 0.0f;
                    yawAdjustment = 0.0f;
                }
                if (Minecraft.currentScreen().isNotNull()) {
                    this.yawIntegral = 0.0f;
                    this.pitchIntegral = 0.0f;
                }
                this.rotationController.setTargetRotation(managedYaw + yawError + yawAdjustment / 3.0f, managedPitch + pitchAdjustment);
            }
            if (RotationManager.INSTANCE.getActiveController() == null || !this.isControllingRotation() && RotationManager.INSTANCE.hasAdaptiveController()) {
                RotationManager.INSTANCE.setController(this.rotationController);
            }
        } else {
            if (this.isHoldingSwitchTarget()) {
                // An out-of-range target is still locked until the deadline,
                // but must not keep an attack ready while outside the range.
                this.readyToAttack = false;
            } else {
                this.resetTargeting();
            }
        }
    }

    @Override
    public void setEnabled(boolean enabledState, boolean bypassVisibilityCheck) {
        this.pitchIntegral = 0.0f;
        this.yawIntegral = 0.0f;
        // Publish the disabled state immediately so the normal module-toggle
        // notification fires even while Aura owns a rotation controller.
        // onDisable still releases that controller into its smooth return.
        super.setEnabled(enabledState, bypassVisibilityCheck);
    }

    private boolean isLookingAtTarget() {
        RayTraceResult rayTraceResult;
        return this.target != null
                && this.isControllingRotation()
                && this.isWithinAttackRange(this.target)
                && (rayTraceResult = RotationManager.INSTANCE.getExtendedReachRayTrace()) != null
                && rayTraceResult.isNotNull()
                && this.target.equals(rayTraceResult.getEntity());
    }

    private boolean isLookingAtTargetIgnoringOtherEntities() {
        if (this.target == null || this.target.isNull()
                || !this.isControllingRotation() || !this.isWithinAttackRange(this.target)) {
            return false;
        }
        final int targetId = this.target.S();
        RayTraceResult targetRayTrace = RotationManager.INSTANCE.rayTraceUsingManagedRotation(
                this.getAttackRange(), 0.0f, true,
                entity -> entity != null && entity.isNotNull() && entity.S() == targetId);
        return targetRayTrace != null
                && targetRayTrace.isNotNull()
                && targetRayTrace.getEntity().isNotNull()
                && targetRayTrace.getEntity().S() == targetId;
    }

    public void resetTargeting() {
        this.restoreClicksAfterAttackPacket();
        this.attackTiming.reset();
        this.legitSwingTargetId = Integer.MIN_VALUE;
        this.failSwingTargetId = Integer.MIN_VALUE;
        this.firstHitPending = false;
        this.pitchIntegral = 0.0f;
        this.yawIntegral = 0.0f;
        this.target = null;
        this.renderTargets.clear();
        this.readyToAttack = false;
        this.clearPendingSwitch();
        if (this.rotationController != null && this.isControllingRotation()) {
            this.rotationController.setScaleAxesProportionally(true);
            this.rotationController.setLinearAcceleration(true);
            RotationManager.INSTANCE.releaseController(this.rotationController);
        }
        if (RotationManager.INSTANCE.getActiveController() == null || RotationManager.INSTANCE.getActiveController() != this.rotationController || this.rotationController != null && this.rotationController.isRelativeMode() && this.rotationController.isComplete()) {
            this.rotationController = null;
            this.rotationClaim.release(this);
        }
    }

    private double getAttackRange() {
        // Acquisition may begin farther away because of Extra swing distance,
        // but the actual attack is legal all the way up to the configured
        // vanilla reach. The previous 2.88 value made Aura acquire targets at
        // three blocks and then reject its own attack before packet dispatch.
        return ACQUISITION_REACH;
    }

    private void resetJitter() {
        // A stable point keeps silent aim from visibly wavering after acquiring a target.
    }

    private boolean shouldSkip() {
        return this.breakingBlocks
                || this.rotationClaim.isBlockedFor(this) && !this.rotationClaim.acquire(this, true);
    }

    @Override
    public void onDisable() {
        this.restoreClicksAfterAttackPacket();
        this.attackTiming.reset();
        this.legitSwingTargetId = Integer.MIN_VALUE;
        this.failSwingTargetId = Integer.MIN_VALUE;
        this.firstHitPending = false;
        if (this.rotationController != null) {
            if (this.isControllingRotation()) {
                RotationManager.INSTANCE.releaseController(this.rotationController);
            }
            this.rotationController = null;
        }
        if (this.perfectSwingAttackPending) {
            AttackKeyController.releaseAttackKey();
            this.perfectSwingAttackPending = false;
        }
        this.clearPendingAuraAttack();
        this.clearPendingSwitch();
        this.prevTargetIds.clear();
        this.renderTargets.clear();
        this.hitCurrentTarget = false;
        this.rotationClaim.release(this);
    }
}
