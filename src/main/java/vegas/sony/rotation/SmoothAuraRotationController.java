package vegas.sony.rotation;

import vegas.sony.utils.MathUtil;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.GuiScreen;

/** Shared Aura mouse steps, bounded across tick and render updates. */
public abstract class SmoothAuraRotationController extends AdaptiveRotationController {
    private long sampledTick = Long.MIN_VALUE;
    private float horizontalLimit;
    private float verticalLimit;
    private double usedYawBudget;
    private double usedPitchBudget;
    private float reportedYawDistance;
    private float reportedPitchDistance;

    public static float degreesPerTick(double aimSpeed) {
        if (!Double.isFinite(aimSpeed)) return 0.0f;
        // Aura's 0.25 mouse steps per update, 50 updates per tick and
        // vanilla's 0.15 degree conversion. Sensitivity is normalized by
        // RotationManager's update count.
        return (float)(Math.max(0.0, Math.min(10.0, aimSpeed)) * 1.875);
    }

    private static float boundedStep(double error, double perTickLimit,
                                     double updatesPerTick, double remainingBudget) {
        if (!Double.isFinite(error) || !Double.isFinite(perTickLimit)) return 0.0f;
        double step = Math.min(Math.abs(error), Math.max(0.0, remainingBudget));
        step = Math.min(step, Math.max(0.0, perTickLimit) / Math.max(1.0, updatesPerTick));
        return (float)Math.copySign(step, error);
    }

    public void beginTick(long tick) {
        if (this.sampledTick == tick) return;
        this.sampledTick = tick;
        double speed = this.getSpeed();
        // Zero holds the attacking aim. A released controller must still
        // be able to return to the player's view.
        if (this.isRelativeMode()) speed = Math.max(1.0, speed);
        this.horizontalLimit = degreesPerTick(speed);
        this.verticalLimit = this.horizontalLimit;
        double quantum = this.getMouseScale() * 0.15;
        this.usedYawBudget = Math.abs(this.pendingYawDelta * quantum);
        this.usedPitchBudget = Math.abs(this.pendingPitchDelta * quantum);
    }

    @Override
    public void update(EntityPlayerSP player, GuiScreen screen) {
        if (player.isNull()) return;
        // Also runs after the owning module releases or disables itself.
        this.beginTick(player.l());
        super.update(player, screen);
    }

    @Override
    public boolean updateYaw() {
        if (this.targetYaw == UNSET_ROTATION) return true;
        double quantum = this.getMouseScale() * 0.15;
        double predicted = this.getCurrentYaw() + this.pendingYawDelta * quantum;
        double error = MathUtil.wrapAngleTo180((float)(this.targetYaw - predicted));
        if (Math.abs(error) <= this.tolerance) return true;
        float step = boundedStep(error, this.horizontalLimit, this.updatesPerTick(),
                this.horizontalLimit - this.usedYawBudget);
        this.pendingYawDelta += (float)(step / quantum);
        this.usedYawBudget += Math.abs(step);
        return false;
    }

    @Override
    public boolean updatePitch() {
        if (this.targetPitch == UNSET_ROTATION) return true;
        double quantum = this.getMouseScale() * 0.15;
        double predicted = this.getCurrentPitch() + this.pendingPitchDelta * quantum;
        double error = this.targetPitch - predicted;
        if (Math.abs(error) <= this.tolerance) return true;
        float step = boundedStep(error, this.verticalLimit, this.updatesPerTick(),
                this.verticalLimit - this.usedPitchBudget);
        this.pendingPitchDelta += (float)(step / quantum);
        this.usedPitchBudget += Math.abs(step);
        return false;
    }

    private double updatesPerTick() {
        return Math.max(1.0, Math.round(50.0 * RotationManager.INSTANCE.getSensitivityTimeScale()));
    }

    protected boolean usesMouseQuantization() {
        return true;
    }

    @Override
    public void applyPendingMovement(GuiScreen screen) {
        if (screen.isNotNull()) {
            this.pendingYawDelta = 0.0f;
            this.pendingPitchDelta = 0.0f;
            return;
        }
        if (this.usesMouseQuantization()) {
            super.applyPendingMovement(screen);
            return;
        }
        float mouseScale = this.getMouseScale();
        float yaw = this.pendingYawDelta * mouseScale;
        float pitch = this.pendingPitchDelta * mouseScale;
        this.applyMouseDelta(yaw, -pitch);
        this.yawDistance += Math.abs(yaw * 0.15f);
        this.pitchDistance += Math.abs(pitch * 0.15f);
        this.pendingYawDelta = 0.0f;
        this.pendingPitchDelta = 0.0f;
    }

    @Override
    public void setRelativeMode(boolean relative) {
        if (this.isRelativeMode() != relative) {
            this.pendingYawDelta = 0.0f;
            this.pendingPitchDelta = 0.0f;
        }
        super.setRelativeMode(relative);
    }

    public float getHorizontalLimit() {
        return this.horizontalLimit;
    }

    public float getVerticalLimit() {
        return this.verticalLimit;
    }

    public float consumeTravel() {
        float travel = this.yawDistance - this.reportedYawDistance
                + this.pitchDistance - this.reportedPitchDistance;
        this.reportedYawDistance = this.yawDistance;
        this.reportedPitchDistance = this.pitchDistance;
        return travel;
    }

    public void holdCurrentRotation() {
        this.pendingYawDelta = 0.0f;
        this.pendingPitchDelta = 0.0f;
        this.setTargetRotation(this.getCurrentYaw(), this.getCurrentPitch());
    }
}
