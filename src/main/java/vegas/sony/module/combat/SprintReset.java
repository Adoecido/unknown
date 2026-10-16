package vegas.sony.module.combat;

import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.TimerUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;

/**
 * Sprint reset adapted from OpenExpo's SprintReset module.
 *
 * <p>The reset is scheduled on the game tick. When target-damage confirmation
 * is enabled, receiving the target's damage packet arms the reset instead of
 * mutating input from the network thread.</p>
 */
public final class SprintReset extends Mod {
    private static final int MODULE_ID = 0x53505254;
    private static final long DAMAGE_CONFIRMATION_TIMEOUT_MS = 750L;

    private final ModeOption noStopMode = new ModeOption("No stop //$ui");
    private final ModeOption legitMode = new ModeOption("Legit //$ui");
    private final ModeValue mode = ModeValue.create(this, "Mode //$ui", this.noStopMode,
            this.noStopMode, this.legitMode);
    private final NumberValue interval = NumberValue.create(this, "Interval //$ui", "#", "ms",
            0.0, 400.0, 2000.0, 1.0);
    private final BooleanValue requireTargetDamage = BooleanValue.create(this,
            "Require target damage //$ui", true);
    private final NumberValue duration = NumberValue.create(this, "Duration //$ui", "#", "ms",
            0.0, 50.0, 200.0, 1.0);
    private final TimerUtil intervalTimer = new TimerUtil();

    private int armedTargetId = -1;
    private long confirmationDeadline;
    private long restoreForwardAt;
    private boolean attackArmed;
    private boolean resetQueued;
    private boolean forwardSuppressed;

    public SprintReset() {
        super("SprintReset //$ui", MODULE_ID, Category.COMBAT,
                "Resets sprint after a confirmed combat hit //$ui");
        this.mode.addActiveMode(this.duration, this.legitMode);
        this.addValue(this.mode, this.interval, this.requireTargetDamage, this.duration);
        this.interval.setMaximumFractionDigits(0);
        this.duration.setMaximumFractionDigits(0);
    }

    @Override
    public String getDetailedSuffix() {
        return this.mode.getDisplayValue();
    }

    @EventHandler
    public void onPreAttack(EventPreAttack event) {
        if (this.confirmationDeadline != 0L || this.resetQueued) {
            this.attackArmed = false;
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        this.attackArmed = !event.isCanceled() && player.isNotNull()
                && player.B$src$Z$f90iek()
                && this.intervalTimer.hasTimeElapsed(this.interval.getValue().longValue());
        this.armedTargetId = this.attackArmed ? event.getTarget().S() : -1;
    }

    @EventHandler
    public void onPostAttack(EventPostAttack event) {
        if (!this.attackArmed || event.getTarget().S() != this.armedTargetId) {
            return;
        }
        this.attackArmed = false;
        if (this.requireTargetDamage.getEffectiveValue().booleanValue()) {
            this.confirmationDeadline = System.currentTimeMillis() + DAMAGE_CONFIRMATION_TIMEOUT_MS;
        } else {
            this.resetQueued = true;
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        if (this.armedTargetId < 0 || this.confirmationDeadline == 0L) {
            return;
        }
        Entity damaged = AttackPacketTimingTracker.getDamagePacketEntity(event.getPacket());
        if (damaged != null && damaged.isNotNull() && damaged.S() == this.armedTargetId) {
            this.resetQueued = true;
            this.confirmationDeadline = 0L;
            this.armedTargetId = -1;
        }
    }

    @EventHandler
    public void onPlayerTick(EventPrePlayerTick event) {
        long now = System.currentTimeMillis();
        if (this.confirmationDeadline != 0L && now >= this.confirmationDeadline) {
            this.clearPendingAttack();
        }
        if (this.forwardSuppressed && now >= this.restoreForwardAt) {
            this.restoreForwardKey();
        }
        if (!this.resetQueued) {
            return;
        }
        this.resetQueued = false;
        this.intervalTimer.reset();
        EntityPlayerSP player = event.getThePlayer();
        if (player.isNull()) {
            return;
        }
        player.R(false);
        if (this.mode.getValue() == this.legitMode) {
            KeyBinding forward = Minecraft.gameSettings().r();
            forward.setPressed(false);
            player.movementInput().B(0.0f);
            player.movementInput().M(0.0f);
            this.forwardSuppressed = true;
            this.restoreForwardAt = now + this.duration.getValue().longValue();
        }
    }

    private void restoreForwardKey() {
        KeyBinding forward = Minecraft.gameSettings().r();
        forward.setPressed(ClientSettings.isPhysicalKeyDown(forward));
        this.forwardSuppressed = false;
        this.restoreForwardAt = 0L;
    }

    private void clearPendingAttack() {
        this.attackArmed = false;
        this.resetQueued = false;
        this.armedTargetId = -1;
        this.confirmationDeadline = 0L;
    }

    @Override
    public void onDisable() {
        if (this.forwardSuppressed && Minecraft.thePlayer().isNotNull()) {
            this.restoreForwardKey();
        }
        this.clearPendingAttack();
    }
}
