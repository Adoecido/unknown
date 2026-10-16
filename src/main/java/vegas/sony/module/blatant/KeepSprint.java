package vegas.sony.module.blatant;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.event.impl.EventSetSprinting;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.utils.MathUtil;
import vegas.sony.utils.TimerUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

/**
 * Undoes the vanilla 1.8 attack slowdown. EntityPlayer#attackTargetEntityWithCurrentItem
 * multiplies motionX/motionZ by 0.6 and calls setSprinting(false) whenever a sprinting
 * hit lands; this module detects that exact multiplication after the fact and scales the
 * motion back up by {@code retainFactor / 0.6}, then restores the sprint flag.
 */
public class KeepSprint extends Mod {
    /** Vanilla multiplies horizontal motion by this constant on a sprint hit. */
    private static final double VANILLA_ATTACK_SLOWDOWN = 0.6;
    private static final long MODULE_ID = -249991328817855756L;

    private final NumberValue retainFactor = new NumberValue(this,
            "Retain factor //$ui", 0.95, 0.6, 1.0, "#.##", "");
    private final BooleanValue resetSprint = new BooleanValue(this, "Reset sprint //$ui", false);
    private final TimerUtil sprintResetTimer = new TimerUtil();

    private double preAttackMotionX;
    private double preAttackMotionZ;
    private boolean cancelNextSprintEnable;
    private boolean sprintResetPending;

    public KeepSprint() {
        super("KeepSprint //$ui", (int)MODULE_ID, Category.COMBAT,
                "Prevents you from losing sprint when attacking //$ui");
        this.addValue(this.retainFactor, this.resetSprint);
    }

    @Override
    public String getDetailedSuffix() {
        return this.retainFactor.getDisplayValue();
    }

    @Override
    public boolean isBlatantMod() {
        return true;
    }

    @EventHandler
    public void onPreAttack(EventPreAttack event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        this.preAttackMotionX = player.t();
        this.preAttackMotionZ = player.T();
    }

    @EventHandler
    public void onPostAttack(EventPostAttack event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || player.F() <= 0.0f) {
            return;
        }
        // Off the ground the slowdown is only worth undoing while the momentum still
        // points forward, otherwise restoring it just amplifies a sideways drift.
        if (!player.b$src$Z$fqlxe4() && !this.isMovingForward(player)) {
            return;
        }
        // Only act when the motion is exactly the pre-attack motion times 0.6: that is
        // the fingerprint of the vanilla slowdown and nothing else. collidedHorizontally
        // is excluded because a wall already rewrote motion this tick.
        if (player.t() == this.preAttackMotionX * VANILLA_ATTACK_SLOWDOWN
                && player.T() == this.preAttackMotionZ * VANILLA_ATTACK_SLOWDOWN
                && !player.r()) {
            double retainedMotionScale = this.retainFactor.getValue();
            player.setMotionX(player.t() / VANILLA_ATTACK_SLOWDOWN * retainedMotionScale);
            player.setMotionZ(player.T() / VANILLA_ATTACK_SLOWDOWN * retainedMotionScale);
            if (this.resetSprint.getEffectiveValue().booleanValue()) {
                // Rate-limited W-tap: drop sprint on the next tick and swallow the
                // re-enable so the server sees a genuine sprint reset.
                if (this.sprintResetTimer.hasTimeElapsed(500L)) {
                    this.sprintResetPending = true;
                    this.sprintResetTimer.reset();
                }
            } else {
                player.R(true);
            }
        }
    }

    @EventHandler
    public void onTick(EventPrePlayerTick event) {
        if (this.sprintResetPending) {
            EntityPlayerSP player = Minecraft.thePlayer();
            if (player.isNotNull()) {
                player.R(false);
                this.cancelNextSprintEnable = true;
            }
            this.sprintResetPending = false;
        }
    }

    @EventHandler
    public void onSetSprinting(EventSetSprinting event) {
        if (this.cancelNextSprintEnable && event.isNewStateSprinting()) {
            event.setCancelled(true);
            this.cancelNextSprintEnable = false;
        }
    }

    /** True when the current horizontal momentum still points along the look vector. */
    private boolean isMovingForward(EntityPlayerSP player) {
        if (player.F() <= 0.0f) {
            return false;
        }
        float yaw = player.J();
        float forwardX = -MathUtil.sin(yaw * (float)Math.PI / 180.0f);
        float forwardZ = MathUtil.cos(yaw * (float)Math.PI / 180.0f);
        return player.t() * (double)forwardX + player.T() * (double)forwardZ > 0.0;
    }

    @Override
    public void onDisable() {
        this.preAttackMotionX = 0.0;
        this.preAttackMotionZ = 0.0;
        this.cancelNextSprintEnable = false;
        this.sprintResetPending = false;
    }
}
