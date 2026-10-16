package vegas.sony.module.combat;

import vegas.sony.combat.CentralCombatTarget;
import vegas.sony.combat.CombatRangePrediction;
import vegas.sony.combat.CombatRangeDebugLog;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public final class TimerRange extends Mod {
    private final BooleanValue staticRange = BooleanValue.create(this, "Static Range //$ui", false);
    private final NumberValue minRange = NumberValue.create(this, "Min Range //$ui", "#.#", "", 1.0, 3.0, 6.0, 0.05);
    private final NumberValue maxRange = NumberValue.create(this, "Max Range //$ui", "#.#", "", 1.0, 4.5, 8.0, 0.05);
    private final BooleanValue smartRange = BooleanValue.create(this, "Smart Range //$ui", true);
    private final NumberValue attackRange = NumberValue.create(this, "Attack Range //$ui", "#.#", "", 1.0, 3.0, 6.0, 0.05);
    private final NumberValue searchRange = NumberValue.create(this, "Search Range //$ui", "#.#", "", 3.0, 8.0, 12.0, 0.1);
    private final NumberValue attackTimer = NumberValue.create(this, "Attack Timer //$ui", "#.#", "x", 1.0, 2.0, 4.0, 0.05);
    private final NumberValue gainTimer = NumberValue.create(this, "Balance Gain Timer //$ui", "#.##", "x", 0.5, 0.95, 0.99, 0.01);
    private final NumberValue maxBalance = NumberValue.create(this, "Max Balance //$ui", "#", "ticks", 1.0, 20.0, 100.0, 1.0);
    private final NumberValue balanceLimitTimer = NumberValue.create(this, "Balance Limit Timer //$ui", "#.##", "x", 0.5, 0.99, 1.0, 0.01);
    private final BooleanValue alwaysGain = BooleanValue.create(this, "Always Gain //$ui", false);
    private final BooleanValue pauseOnFlag = BooleanValue.create(this, "Pause On Flag //$ui", true);
    private double balance;
    private int flagCooldown;
    private int ticksSinceAttack = 1000;
    private float appliedSpeed = 1.0f;
    private long lastSettingsFingerprint = Long.MIN_VALUE;
    private int settingsCooldown;

    public TimerRange() {
        super("TimerRange //$ui", 0, Category.COMBAT, "Uses a bounded timer balance to accelerate predicted entries into combat range. //$ui");
        this.staticRange.addDependentValues(this.minRange, this.maxRange);
        this.staticRange.whenEqualTo(false).applyTo(this.smartRange, this.attackRange);
        this.addValue(this.staticRange, this.minRange, this.maxRange, this.smartRange, this.attackRange,
                this.searchRange, this.attackTimer, this.gainTimer, this.maxBalance,
                this.balanceLimitTimer, this.alwaysGain, this.pauseOnFlag);
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (Minecraft.thePlayer().isNull() || Minecraft.theWorld().isNull()) {
            CombatRangeDebugLog.state("TimerRange", "waiting: player/world unavailable");
            this.resetAll();
            return;
        }
        long settingsFingerprint = this.settingsFingerprint();
        if (this.lastSettingsFingerprint == Long.MIN_VALUE) {
            this.lastSettingsFingerprint = settingsFingerprint;
        } else if (settingsFingerprint != this.lastSettingsFingerprint) {
            this.lastSettingsFingerprint = settingsFingerprint;
            this.balance = 0.0;
            this.settingsCooldown = 10;
            this.apply(1.0f);
            CombatRangeDebugLog.state("TimerRange", "paused: configuration changed, stabilizing at 1.0x");
            return;
        }
        if (this.settingsCooldown > 0) {
            --this.settingsCooldown;
            this.apply(1.0f);
            return;
        }
        double maximum = Math.max(1.0, this.maxBalance.getValue());
        this.balance = clamp(this.balance + this.appliedSpeed - 1.0, -maximum, maximum);
        if (this.ticksSinceAttack < 1000) ++this.ticksSinceAttack;
        if (this.flagCooldown > 0) {
            CombatRangeDebugLog.state("TimerRange", "paused: lagback cooldown " + this.flagCooldown + " ticks");
            --this.flagCooldown;
            this.apply(1.0f);
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        EntityLivingBase target = CentralCombatTarget.get();
        if (target == null) {
            CombatRangeDebugLog.state("TimerRange", this.alwaysGain.isEnabled()
                    ? "charging: no target, Always Gain enabled" : "waiting: no player near the crosshair and no recent attacked target");
            this.apply(this.alwaysGain.isEnabled() && this.balance > -maximum
                    ? this.gainTimer.getValue().floatValue() : 1.0f);
            return;
        }
        double current = CombatRangePrediction.currentDistance(player, target);
        if (!finite(current) || current > this.searchRange.getValue()) {
            CombatRangeDebugLog.state("TimerRange", "waiting: target outside search range");
            this.apply(this.alwaysGain.isEnabled() && this.balance > -maximum
                    ? this.gainTimer.getValue().floatValue() : 1.0f);
            return;
        }
        int predictionTicks = Math.max(1, (int)Math.ceil(this.attackTimer.getValue()));
        CombatRangePrediction.Result prediction = CombatRangePrediction.predict(player, predictionTicks);
        double predicted = prediction.valid
                ? CombatRangePrediction.distanceToTarget(prediction.x, player.N() + player.X() * 0.5, prediction.z, target)
                : Double.NaN;
        boolean approaching = finite(predicted) && predicted + 0.02 < current;
        boolean inRange;
        if (this.staticRange.isEnabled()) {
            double min = Math.min(this.minRange.getValue(), this.maxRange.getValue());
            double max = Math.max(this.minRange.getValue(), this.maxRange.getValue());
            inRange = current >= min && current <= max && approaching;
        } else {
            inRange = approaching && predicted <= this.attackRange.getValue()
                    && (current > this.attackRange.getValue()
                    || this.smartRange.isEnabled() && this.ticksSinceAttack > 7 && current > 2.0);
        }
        float boost = this.attackTimer.getValue().floatValue();
        double projectedBalance = this.balance + boost - 1.0;
        if (inRange && player.c$src$I$15a9iwo() == 0 && projectedBalance <= maximum) {
            CombatRangeDebugLog.state("TimerRange", "active: timer boost " + boost + "x, balance=" + this.balance);
            this.apply(boost);
        } else if (this.balance > -maximum) {
            CombatRangeDebugLog.state("TimerRange", "charging: " + this.gainTimer.getValue() + "x, balance=" + this.balance);
            this.apply(this.gainTimer.getValue().floatValue());
        } else {
            CombatRangeDebugLog.state("TimerRange", "limited: maximum saved balance reached");
            this.apply(this.balanceLimitTimer.getValue().floatValue());
        }
    }

    @EventHandler
    public void onAttack(EventPreAttack event) {
        if (event != null) CentralCombatTarget.record(event.getTarget());
        EntityLivingBase target = CentralCombatTarget.get();
        if (target != null && event.getTarget() != null && !event.getTarget().isNull()
                && target.getObject() == event.getTarget().getObject()) this.ticksSinceAttack = 0;
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        if (!this.pauseOnFlag.isEnabled() || !event.getPacket().isInstance(MappedClasses.zw)) return;
        this.balance = 0.0;
        this.flagCooldown = 20;
        this.apply(1.0f);
    }

    private void apply(float speed) {
        if (Float.isNaN(speed) || Float.isInfinite(speed) || speed <= 0.0f) speed = 1.0f;
        this.appliedSpeed = speed;
        Minecraft.getTimer().setTimerSpeed(speed);
    }

    private void resetAll() {
        this.balance = 0.0;
        this.flagCooldown = 0;
        this.ticksSinceAttack = 1000;
        this.settingsCooldown = 0;
        this.apply(1.0f);
    }

    private long settingsFingerprint() {
        long hash = 17L;
        hash = hash * 31L + (this.staticRange.isEnabled() ? 1L : 0L);
        hash = hash * 31L + Double.doubleToLongBits(this.minRange.getValue());
        hash = hash * 31L + Double.doubleToLongBits(this.maxRange.getValue());
        hash = hash * 31L + (this.smartRange.isEnabled() ? 1L : 0L);
        hash = hash * 31L + Double.doubleToLongBits(this.attackRange.getValue());
        hash = hash * 31L + Double.doubleToLongBits(this.searchRange.getValue());
        hash = hash * 31L + Double.doubleToLongBits(this.attackTimer.getValue());
        hash = hash * 31L + Double.doubleToLongBits(this.gainTimer.getValue());
        hash = hash * 31L + Double.doubleToLongBits(this.maxBalance.getValue());
        hash = hash * 31L + Double.doubleToLongBits(this.balanceLimitTimer.getValue());
        hash = hash * 31L + (this.alwaysGain.isEnabled() ? 1L : 0L);
        hash = hash * 31L + (this.pauseOnFlag.isEnabled() ? 1L : 0L);
        return hash;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    @Override
    public String getSimpleSuffix() {
        return Math.round(this.balance * 10.0) / 10.0 + "t";
    }

    @Override
    public void onDisable() {
        this.resetAll();
    }
}
