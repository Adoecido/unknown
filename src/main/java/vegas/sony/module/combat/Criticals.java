package vegas.sony.module.combat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.PacketSendDispatchGuardCallback;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.control.PrimaryActionControlClaim;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.RotationUtil;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.RandomValue;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;

/**
 * Jump critical helper with two mutually exclusive implementations.
 *
 * <p>Lag delays outgoing packets only after a real jump reaches its descending
 * phase. Timer spends half-speed time on the ascent and pays the exact saved
 * timer balance back at up to 1.5x on the descent.</p>
 */
public final class Criticals extends Mod {
    private static final double MOTION_EPSILON = 0.003;
    private static final float ASCENT_TIMER = 0.5f;
    private static final float MAX_DESCENT_TIMER = 1.5f;
    private static final double MAX_TIMER_BALANCE = 40.0;

    private final ModeOption lagMode = new ModeOption("Lag //$ui");
    private final ModeOption timerMode = new ModeOption("Timer //$ui");
    private final ModeOption blinkMode = new ModeOption("Blink //$ui");
    private final ModeOption timerV2Mode = new ModeOption("Timer V2 //$ui");
    private final ModeValue mode = ModeValue.create(this, "Mode //$ui", this.lagMode,
            this.lagMode, this.timerMode, this.blinkMode, this.timerV2Mode);
    private final NumberValue pingVariation = NumberValue.create(this,
            "Variação de ping //$ui", "#", "ms", 10.0, 30.0, 50.0, 1.0,
            "Atraso acrescentado aos pacotes durante a descida //$ui");
    private final NumberValue chance = NumberValue.create(this, "Chance //$ui", "#", "%",
            1.0, 100.0, 100.0, 1.0,
            "Chance de aplicar o atraso em cada salto //$ui");
    private final RandomValue blinkDelay = RandomValue.createWithIncrement(this,
            "Blink delay //$ui", "#", "ms", 50.0, 300.0, 600.0, 1000.0, 1.0);
    private final NumberValue blinkRange = NumberValue.create(this, "Blink range //$ui", "#.#",
            "", 1.0, 4.0, 6.0, 0.1,
            "Distancia maxima para segurar os pacotes de movimento //$ui");
    private final NumberValue timerV2Speed = NumberValue.create(this, "Timer V2 speed //$ui",
            "#.##", "x", 0.1, 0.8, 1.0, 0.05,
            "Velocidade aplicada durante uma queda capaz de causar critical //$ui");
    private final NumberValue timerV2Range = NumberValue.create(this, "Timer V2 range //$ui",
            "#.#", "", 1.0, 4.0, 6.0, 0.1,
            "Distancia maxima do alvo para ativar o Timer V2 //$ui");

    private final Queue<DelayedPacket> delayedPackets =
            new ConcurrentLinkedQueue<DelayedPacket>();
    private final Queue<DelayedPacket> blinkPackets =
            new ConcurrentLinkedQueue<DelayedPacket>();
    private final Object packetReleaseLock = new Object();
    private final ScheduledExecutorService packetReleaseExecutor =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private final PacketDispatchGuard dispatchGuard = PacketDispatchGuard.b;
    private final PrimaryActionControlClaim primaryClaim = SharedModuleControlClaims.primaryAction;

    private volatile boolean descending;
    private volatile boolean lagThisFall;
    private boolean sawAscent;
    private boolean claimHeld;
    private boolean timerApplied;
    private double timerBalance;
    private boolean blinkActive;
    private long blinkStartedAt;
    private long activeBlinkDelay;
    private int activeMode = -1;

    public Criticals() {
        super("Criticals //$ui", 0, Category.COMBAT,
                "Applies jump critical timing through packet delay or balanced timer speed //$ui");
        this.mode.addModeDependentValues(this.lagMode, this.pingVariation, this.chance);
        this.mode.addModeDependentValues(this.blinkMode, this.blinkDelay, this.blinkRange);
        this.mode.addModeDependentValues(this.timerV2Mode, this.timerV2Speed, this.timerV2Range);
        this.addValue(this.mode, this.pingVariation, this.chance, this.blinkDelay,
                this.blinkRange, this.timerV2Speed, this.timerV2Range);
        this.pingVariation.setMaximumFractionDigits(0);
        this.chance.setMaximumFractionDigits(0);
        this.blinkDelay.setMaximumFractionDigits(0);
    }

    @Override
    public void onEnable() {
        this.activeMode = this.mode.getSelectedIndex();
        this.clearJumpState();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTick(EventPreTick event) {
        int selectedMode = this.mode.getSelectedIndex();
        if (selectedMode != this.activeMode) {
            this.resetRuntimeState();
            this.activeMode = selectedMode;
        }

        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || Minecraft.theWorld().isNull()) {
            this.resetRuntimeState();
            return;
        }

        if (this.mode.getValue() == this.lagMode) {
            this.handleLagMode(player);
        } else if (this.mode.getValue() == this.timerMode) {
            this.handleTimerMode(player);
        } else if (this.mode.getValue() == this.blinkMode) {
            this.handleBlinkMode(player);
        } else {
            this.handleTimerV2Mode(player);
        }
    }

    private void handleLagMode(EntityPlayerSP player) {
        double motionY = player.q();
        if (player.boolean_b()) {
            this.stopLag();
            this.clearJumpState();
            return;
        }
        if (motionY > MOTION_EPSILON) {
            this.sawAscent = true;
            this.descending = false;
            this.lagThisFall = false;
            this.stopLag();
            return;
        }
        if (motionY >= -MOTION_EPSILON || !this.sawAscent) {
            return;
        }
        if (this.descending) {
            return;
        }

        this.descending = true;
        this.lagThisFall = this.rollChance();
        if (!this.lagThisFall) {
            return;
        }
        if (this.primaryClaim.isClaimed()) {
            this.lagThisFall = false;
            return;
        }
        this.primaryClaim.markClaimed();
        this.claimHeld = true;
    }

    private void handleTimerMode(EntityPlayerSP player) {
        double motionY = player.q();
        if (player.boolean_b()) {
            this.timerBalance = 0.0;
            this.sawAscent = false;
            this.restoreTimer();
            return;
        }

        if (motionY > MOTION_EPSILON) {
            this.sawAscent = true;
            this.timerBalance = Math.max(-MAX_TIMER_BALANCE,
                    this.timerBalance + ASCENT_TIMER - 1.0f);
            this.applyTimer(ASCENT_TIMER);
            return;
        }

        if (motionY < -MOTION_EPSILON && this.sawAscent && this.timerBalance < -0.0001) {
            float repayment = (float)Math.min(MAX_DESCENT_TIMER - 1.0f,
                    -this.timerBalance);
            float speed = 1.0f + repayment;
            this.timerBalance = Math.min(0.0, this.timerBalance + repayment);
            this.applyTimer(speed);
            return;
        }

        this.restoreTimer();
    }

    private void handleBlinkMode(EntityPlayerSP player) {
        if (!this.hasTargetInRange(this.blinkRange.getValue().doubleValue())
                || this.isNaturalCritical(player)) {
            this.stopBlink();
            return;
        }

        long now = System.currentTimeMillis();
        if (this.blinkActive && now - this.blinkStartedAt >= this.activeBlinkDelay) {
            this.stopBlink();
            return;
        }
        if (this.blinkActive || this.primaryClaim.isClaimed()) {
            return;
        }

        this.primaryClaim.markClaimed();
        this.claimHeld = true;
        this.blinkActive = true;
        this.blinkStartedAt = now;
        this.activeBlinkDelay = Math.max(50L, Math.round(this.blinkDelay.getRandomValue()));
    }

    private void handleTimerV2Mode(EntityPlayerSP player) {
        if (this.isNaturalCritical(player)
                && this.hasTargetInRange(this.timerV2Range.getValue().doubleValue())) {
            this.applyTimer(this.timerV2Speed.getValue().floatValue());
        } else {
            this.restoreTimer();
        }
    }

    private boolean hasTargetInRange(double range) {
        EntityLivingBase target = RotationUtil.u(range, 180.0);
        return target != null && !target.isNull();
    }

    private boolean isNaturalCritical(EntityPlayerSP player) {
        return !player.boolean_b() && player.q() < -MOTION_EPSILON
                && !player.h$src$Z$ftwoya() && !player.Q$src$Z$fh9faz();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPacketSend(EventPacketSend event) {
        if (this.mode.getValue() == this.blinkMode) {
            this.handleBlinkPacket(event);
            return;
        }
        if (this.mode.getValue() != this.lagMode || !this.descending || !this.lagThisFall
                || event.isCanceled() || event.wasModified()) {
            return;
        }

        Packet packet = event.getPacket();
        if (this.dispatchGuard.R(packet)) {
            return;
        }

        DelayedPacket delayed = new DelayedPacket(new PacketSendDispatchGuardCallback(event));
        synchronized (this.packetReleaseLock) {
            if (!this.descending || !this.lagThisFall || this.mode.getValue() != this.lagMode) {
                this.dispatchGuard.J(packet);
                return;
            }
            this.delayedPackets.add(delayed);
            event.setCancelled(true);
        }
        long delayMillis = this.pingVariation.getValue().longValue();
        this.packetReleaseExecutor.schedule(() -> this.release(delayed), delayMillis,
                TimeUnit.MILLISECONDS);
    }

    private void handleBlinkPacket(EventPacketSend event) {
        if (!this.blinkActive || event.isCanceled() || event.wasModified()) {
            return;
        }
        Packet packet = event.getPacket();
        if (MappedClasses.qD == null || !packet.isInstance(MappedClasses.qD)) {
            return;
        }
        if (this.dispatchGuard.R(packet)) {
            return;
        }

        DelayedPacket delayed = new DelayedPacket(new PacketSendDispatchGuardCallback(event));
        synchronized (this.packetReleaseLock) {
            if (!this.blinkActive || this.mode.getValue() != this.blinkMode) {
                this.dispatchGuard.J(packet);
                return;
            }
            this.blinkPackets.add(delayed);
            event.setCancelled(true);
        }
    }

    private boolean rollChance() {
        double configuredChance = this.chance.getValue();
        if (configuredChance >= 100.0) return true;
        if (configuredChance <= 0.0) return false;
        return ThreadLocalRandom.current().nextDouble(100.0) < configuredChance;
    }

    private void release(DelayedPacket delayed) {
        synchronized (this.packetReleaseLock) {
            if (this.delayedPackets.remove(delayed)) {
                delayed.callback.dispatch(this.dispatchGuard);
            }
        }
    }

    private void flushPackets() {
        synchronized (this.packetReleaseLock) {
            DelayedPacket delayed;
            while ((delayed = this.delayedPackets.poll()) != null) {
                delayed.callback.dispatch(this.dispatchGuard);
            }
        }
    }

    private void flushBlinkPackets() {
        synchronized (this.packetReleaseLock) {
            DelayedPacket delayed;
            while ((delayed = this.blinkPackets.poll()) != null) {
                delayed.callback.dispatch(this.dispatchGuard);
            }
        }
    }

    private void stopBlink() {
        this.blinkActive = false;
        this.blinkStartedAt = 0L;
        this.activeBlinkDelay = 0L;
        this.flushBlinkPackets();
        if (this.claimHeld) {
            this.primaryClaim.clearClaimed();
            this.claimHeld = false;
        }
    }

    private void stopLag() {
        this.descending = false;
        this.lagThisFall = false;
        this.flushPackets();
        if (this.claimHeld) {
            this.primaryClaim.clearClaimed();
            this.claimHeld = false;
        }
    }

    private void applyTimer(float speed) {
        this.timerApplied = true;
        Minecraft.getTimer().setTimerSpeed(speed);
    }

    private void restoreTimer() {
        if (!this.timerApplied) {
            return;
        }
        Minecraft.getTimer().setTimerSpeed(1.0f);
        this.timerApplied = false;
    }

    private void clearJumpState() {
        this.sawAscent = false;
        this.descending = false;
        this.lagThisFall = false;
        this.timerBalance = 0.0;
    }

    private void resetRuntimeState() {
        this.stopLag();
        this.stopBlink();
        this.restoreTimer();
        this.clearJumpState();
    }

    @Override
    public String getSimpleSuffix() {
        if (this.mode.getValue() == this.lagMode) {
            return this.pingVariation.getDisplayValue() + "ms";
        }
        if (this.mode.getValue() == this.blinkMode) {
            return this.blinkDelay.getDisplayValue();
        }
        if (this.mode.getValue() == this.timerV2Mode) {
            return "Timer V2 //$ui";
        }
        return "Timer //$ui";
    }

    @Override
    public void onDisable() {
        this.resetRuntimeState();
        this.activeMode = -1;
    }

    private static final class DelayedPacket {
        private final PacketSendDispatchGuardCallback callback;

        private DelayedPacket(PacketSendDispatchGuardCallback callback) {
            this.callback = callback;
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "PacketRelease");
            thread.setDaemon(true);
            return thread;
        }
    }
}
