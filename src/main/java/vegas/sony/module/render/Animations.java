package vegas.sony.module.render;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.render.animations.AnimationsBlockingState;
import vegas.sony.module.render.animations.AnimationsMode;
import vegas.sony.module.render.animations.DamageResponsiveAnimationsMode;
import vegas.sony.module.render.animations.LegacyBlockingPacketBufferedAnimationsMode;
import vegas.sony.module.render.animations.SwordUseMouseGuardAnimationsMode;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.SubModuleValue;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.Minecraft;

public class Animations
extends Mod {
    private static final long MODULE_COLOR = -1033271949203766542L;
    private final ModeValue mode;
    private final SubModuleValue<DamageResponsiveAnimationsMode> predictiveMode;
    private final SubModuleValue<AnimationsBlockingState> manualMode = new AnimationsBlockingState(this, "Manual").getSelectionValue();
    private final SubModuleValue<LegacyBlockingPacketBufferedAnimationsMode> lagMode;
    public final BooleanValue requireMouseDown;
    public final BooleanValue ignoreManualBlock;
    public final NumberValue targetDistance;
    private final SubModuleValue<AnimationsMode> automaticMode;
    public final NumberValue targetAngle;

    public boolean isHoldingSword() {
        if (Minecraft.currentScreen().isNotNull()) {
            return false;
        }
        return Minecraft.thePlayer().getHeldItemHand().isNotNull() && ItemStackScoreUtil.h(Minecraft.thePlayer().getHeldItemHand().getItem());
    }

    public boolean shouldBlock() {
        return this.isEnabled() && this.getActiveMode().shouldBlock();
    }


    public boolean requiresMouseDown() {
        return this.requireMouseDown.getEffectiveValue();
    }

    @Override
    public String getDetailedSuffix() {
        if (this.mode.getValue() == this.manualMode) {
            return this.manualMode.getInstance().getDetailedSuffix();
        }
        if (this.mode.getValue() == this.lagMode) {
            return this.lagMode.getInstance().getDetailedSuffix();
        }
        return this.getSimpleSuffix();
    }

    public EntityLivingBase findTarget(double angle, double distance) {
        EntityLivingBase target = RotationUtil.u(distance, angle / 2.0);
        if (target == null) {
            return null;
        }
        if (RotationUtil.o(Minecraft.thePlayer(), target, distance, angle / 2.0, true)) {
            return target;
        }
        return null;
    }

    public Animations() {
        super("BlockHit //$ui", (int)MODULE_COLOR, Category.COMBAT, "Automatically blockhit //$ui");
        this.predictiveMode = new DamageResponsiveAnimationsMode(this, "Predict").getSelectionValue();
        this.automaticMode = new SwordUseMouseGuardAnimationsMode(this, "Auto").getSelectionValue();
        this.lagMode = new LegacyBlockingPacketBufferedAnimationsMode(this, "Lag").getSelectionValue();
        this.requireMouseDown = BooleanValue.create(this, "Require mouse down //$ui", true, "Require block to be pressed to blockhit //$ui");
        this.ignoreManualBlock = BooleanValue.create(this, "Ignore manual block //$ui", true, "Prevents manually blocking, useful for holding right click to activate //$ui");
        this.targetAngle = NumberValue.createWithDescription(this, "Angle //$ui", "#", "", 0.0, 90.0, 360.0, "Max target angle to blockhit //$ui");
        this.targetDistance = NumberValue.createWithDescription(this, "Distance //$ui", "#.#", "", 0.0, 5.0, 6.0, "Max target distance to blockhit //$ui");
        this.mode = ModeValue.create((Object)this, "Mode //$ui", this.manualMode, this.manualMode, this.predictiveMode, this.automaticMode, this.lagMode);
        this.mode.setDescription("Manual: Blockhit based on your CPS\nPredict: Predicts when a player can hit you and blocks ahead of time\nAuto: Legacy auto mode from AutoClicker\nLag: Lags you after blocking to maximize server side block time //$ui");
        this.requireMouseDown.addDependentValues(this.ignoreManualBlock);
        this.mode.addModeDependentValues(this.lagMode, this.ignoreManualBlock);
        this.mode.addModeDependentValues(this.lagMode, this.targetAngle);
        this.mode.addModeDependentValues(this.lagMode, this.targetDistance);
        this.addValue(this.mode, this.requireMouseDown, this.ignoreManualBlock, this.targetAngle, this.targetDistance);
    }

    public AnimationsMode getActiveMode() {
        return (AnimationsMode)((SubModuleValue)this.mode.getValue()).getInstance();
    }

    public boolean isBlocking() {
        return this.getActiveMode().isBlocking();
    }

    @Override
    public String getSimpleSuffix() {
        return this.mode.getDisplayValue();
    }

    public EntityLivingBase findDefaultTarget() {
        return this.findTarget(90.0, 5.0);
    }
}
