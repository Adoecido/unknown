package vegas.sony.module.combat;

import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.TimerUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.RandomValue;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.MovementInput;

import java.util.Random;

/**
 * Knockback boost modelled after the SuperKnockback module of FDPClient.
 *
 * <p>WTap is the mode the module is built around: a hit releases the forward
 * sprint state for a few ticks and the next hit lands with the sprint reset
 * knockback bonus. The wait before the reset and the time it stays released
 * are randomised ranges, scaled by how close the target
 * is to the configured ideal distance, the same way the original does it.
 *
 * <p>SprintTap performs the same reset without releasing the forward key: it
 * briefly drops sprint and restores it on the next tick when the player is
 * still moving forward.
 */
public final class SuperKnockback extends Mod {
    private static final int MODULE_COLOR = 0x53554B42;
    private final ModeOption wTapMode = new ModeOption("WTap //$ui");
    private final ModeOption sprintTapMode = new ModeOption("SprintTap //$ui");
    private final ModeValue mode = ModeValue.create(this, "Mode //$ui",
            this.wTapMode, this.wTapMode, this.sprintTapMode);
    private final NumberValue chance = NumberValue.create(this, "Chance //$ui", "#", "%",
            0.0, 100.0, 100.0, 1.0,
            "Percentage of hits that get the knockback boost //$ui");
    private final NumberValue delay = NumberValue.create(this, "Delay //$ui", "#", "ms",
            0.0, 0.0, 500.0, 10.0,
            "Minimum time between two boosts //$ui");
    private final NumberValue hurtTime = NumberValue.create(this, "Hurt time //$ui", "#", "",
            0.0, 10.0, 10.0, 1.0,
            "Skips the boost while the target is above this hurt time //$ui");
    private final RandomValue blockDelayTicks = RandomValue.createWithDescription(this,
            "Ticks until block //$ui", "#", "", 0.0, 0.0, 2.0, 5.0, 1.0,
            "Ticks between the hit and the sprint reset //$ui");
    private final RandomValue blockDurationTicks = RandomValue.createWithDescription(this,
            "Re-sprint ticks //$ui", "#", "", 1.0, 1.0, 2.0, 5.0, 1.0,
            "Ticks before sprinting again //$ui");
    private final NumberValue targetDistance = NumberValue.create(this, "Target distance //$ui",
            "#.#", "", 1.0, 3.0, 5.0, 0.1,
            "Distance at which the tap timing is at its fastest //$ui");
    private final BooleanValue onlyGround = BooleanValue.create(this, "Only ground //$ui", false,
            "Only boosts while standing on the ground //$ui");
    private final BooleanValue onlyMove = BooleanValue.create(this, "Only move //$ui", true,
            "Only boosts while moving //$ui");
    private final BooleanValue onlyMoveForward = BooleanValue.create(this, "Only move forward //$ui",
            true, "Only boosts while walking straight forward //$ui");

    private final Random random = new Random();
    private final TimerUtil boostTimer = new TimerUtil();

    private boolean waitingToBlock;
    private boolean inputBlocked;
    private boolean forwardSuppressed;
    private int ticksUntilBlock;
    private int ticksBlocked;
    private int elapsedTicks;
    private int sprintTapTicks;
    private boolean sprintTapReleased;

    public SuperKnockback() {
        super("SuperKB //$ui", MODULE_COLOR, Category.COMBAT,
                "Boosts the knockback you deal by resetting your sprint on hit //$ui");
        this.chance.setMaximumFractionDigits(0);
        this.delay.setMaximumFractionDigits(0);
        this.hurtTime.setMaximumFractionDigits(0);
        this.mode.addActiveMode(this.blockDelayTicks, this.wTapMode);
        this.mode.addActiveMode(this.blockDurationTicks, this.wTapMode);
        this.mode.addActiveMode(this.targetDistance, this.wTapMode);
        this.onlyMove.addDependentValues(this.onlyMoveForward);
        this.addValue(this.mode, this.chance, this.delay, this.hurtTime,
                this.blockDelayTicks, this.blockDurationTicks, this.targetDistance,
                this.onlyGround, this.onlyMove, this.onlyMoveForward);
    }

    @Override
    public boolean matchesName(String candidate) {
        return super.matchesName(candidate)
                || "SuperKnockback".equalsIgnoreCase(candidate)
                || "SuperKnockBack".equalsIgnoreCase(candidate);
    }

    @Override
    public String getSimpleSuffix() {
        return this.mode.getDisplayValue();
    }

    @EventHandler
    public void onPreAttack(EventPreAttack event) {
        if (event.isCanceled()) {
            return;
        }
        Entity target = event.getTarget();
        if (target.isNull() || !target.isInstance(MappedClasses.zm)) {
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        EntityLivingBase living = new EntityLivingBase(target.getObject());
        if (!this.passesConditions(player, living)) {
            return;
        }
        if (this.mode.getValue() == this.sprintTapMode) {
            if (!player.B$src$Z$f90iek()) {
                return;
            }
            // SprintTap is intentionally a local two-tick reset: release sprint
            // for one tick, then restore it only while the player is moving
            // forward. This mirrors the FDP behaviour without packet spoofing.
            this.sprintTapTicks = 2;
            this.sprintTapReleased = false;
            this.boostTimer.reset();
            return;
        }
        // The tap only helps while the sprint is actually running, and starting
        // a second one on top of a running tap would just keep the player
        // walking instead of sprinting.
        if (!player.B$src$Z$f90iek() || this.inputBlocked || this.waitingToBlock) {
            return;
        }
        double distance = player.getDistanceToEntity(living);
        double timingScale = 1.0 / (Math.abs(this.targetDistance.getValue() - distance) + 1.0);
        this.ticksUntilBlock = (int)(this.randomTicks(this.blockDelayTicks) * timingScale);
        this.ticksBlocked = Math.max(1,
                (int)Math.round(this.randomTicks(this.blockDurationTicks) * timingScale));
        this.elapsedTicks = 0;
        if (this.ticksUntilBlock <= 0) {
            this.beginBlock(player);
        } else {
            this.waitingToBlock = true;
        }
        this.boostTimer.reset();
    }

    @EventHandler
    public void onPlayerTick(EventPrePlayerTick event) {
        EntityPlayerSP player = event.getThePlayer();
        if (player.isNull()) {
            this.resetState();
            return;
        }
        if (this.mode.getValue() == this.sprintTapMode) {
            this.updateSprintTap(player);
            return;
        }
        if (this.inputBlocked) {
            this.suppressForward(player);
            if (++this.elapsedTicks >= this.ticksBlocked) {
                this.endBlock();
            }
            return;
        }
        if (this.waitingToBlock && ++this.elapsedTicks > this.ticksUntilBlock) {
            this.waitingToBlock = false;
            this.beginBlock(player);
        }
    }

    @Override
    public void onDisable() {
        if (this.forwardSuppressed && Minecraft.thePlayer().isNotNull()) {
            this.restoreForward(Minecraft.thePlayer());
        }
        if (this.sprintTapReleased && Minecraft.thePlayer().isNotNull()) {
            this.restoreSprint(Minecraft.thePlayer());
        }
        this.resetState();
    }

    private boolean passesConditions(EntityPlayerSP player, EntityLivingBase target) {
        if (!this.boostTimer.hasTimeElapsed(this.delay.getValue().longValue())) {
            return false;
        }
        try {
            if (target.c$src$I$15a9iwo() > this.hurtTime.getValue().intValue()) {
                return false;
            }
        } catch (Throwable ignored) {
            // A missing hurt time mapping must not disable the module.
        }
        if (this.onlyGround.getEffectiveValue().booleanValue() && !player.b$src$Z$fqlxe4()) {
            return false;
        }
        if (this.onlyMove.getEffectiveValue().booleanValue()) {
            MovementInput input = player.movementInput();
            if (input == null || input.isNull()
                    || input.D() == 0.0f && input.T() == 0.0f) {
                return false;
            }
            if (this.onlyMoveForward.getEffectiveValue().booleanValue() && input.T() != 0.0f) {
                return false;
            }
        }
        return this.random.nextInt(100) < this.chance.getValue().intValue();
    }

    private double randomTicks(RandomValue range) {
        int minimum = range.getMinimumInt();
        int maximum = Math.max(minimum, range.getMaximumInt());
        return minimum == maximum
                ? minimum : minimum + this.random.nextInt(maximum - minimum + 1);
    }

    private void beginBlock(EntityPlayerSP player) {
        this.inputBlocked = true;
        this.elapsedTicks = 0;
        this.suppressForward(player);
    }

    private void endBlock() {
        this.inputBlocked = false;
        this.elapsedTicks = 0;
        if (Minecraft.thePlayer().isNotNull()) {
            this.restoreForward(Minecraft.thePlayer());
        }
    }

    private void suppressForward(EntityPlayerSP player) {
        // Do not forge the movement input while the physical W key remains
        // down. CheatBreaker compares those two sources and reports input
        // simulation. A normal sprint-state reset produces the same combat
        // transition without creating an impossible key/movement pair.
        player.R(false);
        this.forwardSuppressed = true;
    }

    private void restoreForward(EntityPlayerSP player) {
        MovementInput input = player.movementInput();
        if (input != null && input.isNotNull()
                && input.D() > 0.8f
                && ClientSettings.isPhysicalKeyDown(Minecraft.gameSettings().Y())) {
            player.R(true);
        }
        this.forwardSuppressed = false;
    }

    private void updateSprintTap(EntityPlayerSP player) {
        if (this.sprintTapTicks == 2) {
            player.R(false);
            this.sprintTapReleased = true;
            --this.sprintTapTicks;
            return;
        }
        if (this.sprintTapTicks == 1) {
            this.restoreSprint(player);
            this.sprintTapTicks = 0;
        }
    }

    private void restoreSprint(EntityPlayerSP player) {
        MovementInput input = player.movementInput();
        if (input != null && input.isNotNull() && input.D() > 0.8f) {
            player.R(true);
        }
        this.sprintTapReleased = false;
    }

    private void resetState() {
        this.waitingToBlock = false;
        this.inputBlocked = false;
        this.ticksUntilBlock = 0;
        this.ticksBlocked = 0;
        this.elapsedTicks = 0;
        this.sprintTapTicks = 0;
        this.sprintTapReleased = false;
    }
}
