package vegas.sony.module.combat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import vegas.sony.SonyVegas;
import vegas.sony.combat.CentralCombatTarget;
import vegas.sony.combat.CombatRangePrediction;
import vegas.sony.combat.CombatRangeDebugLog;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.PacketSendDispatchGuardCallback;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventTickBase;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.control.PrimaryActionControlClaim;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.UseEntityPacketBridge;

public final class LagRange extends Mod {
    private final NumberValue delay = NumberValue.create(this, "Delay //$ui", "#", "ms", 0.0, 50.0, 500.0, 5.0);
    private final NumberValue attackRange = NumberValue.create(this, "Attack Range //$ui", "#.#", "", 2.0, 3.0, 6.0, 0.05);
    private final NumberValue stopRange = NumberValue.create(this, "Stop Range //$ui", "#.#", "", 1.0, 2.5, 6.0, 0.05);
    private final NumberValue searchRange = NumberValue.create(this, "Search Range //$ui", "#.#", "", 3.0, 7.0, 12.0, 0.1);
    private final NumberValue predictionTicks = NumberValue.create(this, "Prediction Ticks //$ui", "#", "", 1.0, 4.0, 12.0, 1.0);
    private final NumberValue maxHold = NumberValue.create(this, "Max Hold //$ui", "#", "ms", 50.0, 300.0, 1000.0, 10.0);
    private final NumberValue hurtTimeToStop = NumberValue.create(this, "Hurt Time To Stop //$ui", "#", "", 0.0, 0.0, 10.0, 1.0);
    private final Queue<PacketSendDispatchGuardCallback> queue = new ConcurrentLinkedQueue<PacketSendDispatchGuardCallback>();
    private final PacketDispatchGuard dispatchGuard = PacketDispatchGuard.b;
    private final PrimaryActionControlClaim primaryClaim = SharedModuleControlClaims.primaryAction;
    private long eligibleSince;
    private long holdStarted;
    private boolean choking;
    private boolean claimHeld;

    public LagRange() {
        super("LagRange //$ui", 0, Category.NETWORK, "Queues outgoing packets when predicted movement enters the central target's range. //$ui");
        this.addValue(this.delay, this.attackRange, this.stopRange, this.searchRange,
                this.predictionTicks, this.maxHold, this.hurtTimeToStop);
    }

    @EventHandler
    public void onAttack(EventPreAttack event) {
        if (event != null) CentralCombatTarget.record(event.getTarget());
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (this.claimHeld && this.primaryClaim.isClaimed() && !this.choking) this.releaseClaim();
        if (this.claimHeld && !this.primaryClaim.isClaimed()) {
            this.claimHeld = false;
            this.flush();
            return;
        }
        boolean eligible = this.isPredictionEligible();
        long now = System.currentTimeMillis();
        if (!eligible) {
            CombatRangeDebugLog.state("LagRange", this.eligibilityReason());
            this.eligibleSince = 0L;
            this.flush();
            return;
        }
        CombatRangeDebugLog.state("LagRange", this.choking ? "active: holding " + this.queue.size() + " packets" : "armed: prediction enters attack range");
        if (this.eligibleSince == 0L) this.eligibleSince = now;
        if (!this.choking && now - this.eligibleSince >= this.delay.getValue().longValue()) {
            if (!this.primaryClaim.isClaimed()) {
                this.primaryClaim.markClaimed();
                this.claimHeld = true;
                this.choking = true;
                this.holdStarted = now;
                CombatRangeDebugLog.state("LagRange", "active: packet queue started");
            }
        }
        if (this.choking && now - this.holdStarted >= this.maxHold.getValue().longValue()) this.flush();
    }

    private boolean isPredictionEligible() {
        if (Minecraft.thePlayer().isNull() || Minecraft.theWorld().isNull()) return false;
        EntityPlayerSP player = Minecraft.thePlayer();
        EntityLivingBase target = CentralCombatTarget.get();
        if (target == null || player.c$src$I$15a9iwo() > this.hurtTimeToStop.getValue().intValue()) return false;
        double current = CombatRangePrediction.currentDistance(player, target);
        if (!finite(current) || current > this.searchRange.getValue() || current < this.stopRange.getValue()) return false;
        CombatRangePrediction.Result predicted = CombatRangePrediction.predict(player, this.predictionTicks.getValue().intValue());
        if (!predicted.valid) return false;
        double future = CombatRangePrediction.distanceToTarget(predicted.x,
                player.N() + player.X() * 0.5, predicted.z, target);
        return finite(future) && future <= this.attackRange.getValue() && future + 0.02 < current;
    }

    private String eligibilityReason() {
        if (Minecraft.thePlayer().isNull() || Minecraft.theWorld().isNull()) return "waiting: player/world unavailable";
        EntityLivingBase target = CentralCombatTarget.get();
        if (target == null) return "waiting: no player near the crosshair and no recent attacked target";
        if (Minecraft.thePlayer().c$src$I$15a9iwo() > this.hurtTimeToStop.getValue().intValue()) return "paused: self hurtTime above limit";
        double current = CombatRangePrediction.currentDistance(Minecraft.thePlayer(), target);
        if (current > this.searchRange.getValue()) return "waiting: target outside search range";
        if (current < this.stopRange.getValue()) return "waiting: already inside stop range";
        return "waiting: prediction is not entering attack range";
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPacketSend(EventPacketSend event) {
        if (!this.choking || event.isCanceled() || event.wasModified()) return;
        Packet packet = event.getPacket();
        if (this.dispatchGuard.R(packet)) return;
        if (!Thread.currentThread().equals(EventTickBase.PRE_TICK_EXECUTOR.getOwnerThread())) {
            this.flush();
            return;
        }
        this.queue.add(new PacketSendDispatchGuardCallback(event));
        event.setCancelled(true);
        if (UseEntityPacketBridge.isUseEntityPacket(packet)
                && new UseEntityPacketBridge(packet.getObject()).isAttack()) {
            this.flush();
        }
    }

    private void flush() {
        if (!Thread.currentThread().equals(EventTickBase.PRE_TICK_EXECUTOR.getOwnerThread())) {
            EventTickBase.PRE_TICK_EXECUTOR.execute(this::flush);
            return;
        }
        PacketSendDispatchGuardCallback callback;
        while ((callback = this.queue.poll()) != null) {
            // Guarded dispatch writes straight to Netty and deliberately skips
            // EventPacketSend listeners. Recheck the Aura's captured target
            // here so a queued click cannot outlive its legal reach window.
            SilentAura aura = SonyVegas.INSTANCE.getModManager().getMod(SilentAura.class);
            if (aura != null && !aura.mayDispatchDeferredAttack(callback.getPacket())) {
                continue;
            }
            callback.dispatch(this.dispatchGuard);
        }
        if (this.choking) CombatRangeDebugLog.state("LagRange", "released: queued packets sent");
        this.choking = false;
        this.holdStarted = 0L;
        this.releaseClaim();
    }

    private void releaseClaim() {
        if (!this.claimHeld) return;
        this.primaryClaim.clearClaimed();
        this.claimHeld = false;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    @Override
    public String getSimpleSuffix() {
        return this.choking ? "Holding " + this.queue.size() : "Ready";
    }

    @Override
    public void onDisable() {
        this.eligibleSince = 0L;
        this.flush();
    }
}
