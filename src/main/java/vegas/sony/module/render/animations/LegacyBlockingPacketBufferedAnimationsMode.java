package vegas.sony.module.render.animations;

import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.KeyBindingInputState;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.control.PrimaryActionControlClaim;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.module.render.Animations;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.value.RandomValue;
import vegas.sony.wrapper.impl.CPacketPlayerDigging;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LegacyBlockingPacketBufferedAnimationsMode
extends AnimationsMode {
    private final Queue<EventPacketSend> queuedPackets;
    private int delayMillis;
    private final PrimaryActionControlClaim primaryActionClaim;
    private boolean blockCycleCompleted;
    private final TimerUtil bufferTimer;
    private boolean blocking;
    private final RandomValue delay;
    private volatile boolean bufferingPackets;
    private final PacketDispatchGuard dispatchGuard = PacketDispatchGuard.b;

    @EventHandler
    public void onTick(EventPreTick event) {
        if (Minecraft.thePlayer().isNull()) {
            this.stopBuffering(true);
            return;
        }
        EntityLivingBase target = this.findTarget();
        if (this.bufferingPackets && (this.bufferTimer.hasTimeElapsed(this.delayMillis)
                || target == null || !this.isHoldingSword())) {
            this.stopBuffering(true);
            return;
        }
        if (!InputEventDispatcher.getInstance().getFocusState().isFocused()) {
            return;
        }
        if (target != null) {
            boolean shouldBlock = this.isHoldingSword();
            if (((Animations)this.getParent()).requiresMouseDown() && !ClientSettings.isUseItemButtonDown()) {
                shouldBlock = false;
            }
            if (event.getThePlayer().c$src$I$15a9iwo() > AttackPacketTimingTracker.INSTANCE.getExpectedHurtTimeTicks() + 1) {
                this.setBlocking(false);
                this.blockCycleCompleted = false;
                return;
            }
            if (shouldBlock) {
                if (this.bufferingPackets) {
                    if (this.bufferTimer.hasTimeElapsed(Math.max(0, this.delayMillis - 50))) {
                        this.setBlocking(true);
                    }
                    return;
                }
                if (!this.blockCycleCompleted) {
                    if (!this.blocking) {
                        this.setBlocking(true);
                    } else {
                        // Start the lag window before releasing the use-item key. Depending on
                        // the client/runtime, the release packet may be sent synchronously here
                        // or canceled by NoItemRelease, so using that packet as the sole trigger
                        // made Lag mode silently behave like ordinary visual blockhit.
                        this.beginBuffering(null);
                        this.setBlocking(false);
                        this.blockCycleCompleted = true;
                    }
                } else if (!this.blocking && !event.getThePlayer().o$src$Z$1iprrmi()) {
                    this.blockCycleCompleted = false;
                }
            } else if (this.blocking) {
                this.setBlocking(false);
                this.blockCycleCompleted = false;
            }
            return;
        }
        if (this.isHoldingSword() && ((Animations)this.getParent()).requiresMouseDown() && ((Animations)this.getParent()).ignoreManualBlock.getEffectiveValue().booleanValue() && !this.bufferingPackets && ClientSettings.isUseItemButtonDown() && Minecraft.thePlayer().o$src$Z$1iprrmi()) {
            KeyBindingInputState.sendRightButtonUp();
            this.blocking = false;
            this.blockCycleCompleted = false;
            return;
        }
        if (event.getThePlayer().c$src$I$15a9iwo() > AttackPacketTimingTracker.INSTANCE.getExpectedHurtTimeTicks() + 1) {
            this.setBlocking(false);
            this.blockCycleCompleted = false;
            return;
        }
        if (this.blocking) {
            this.setBlocking(false);
            this.blockCycleCompleted = false;
        }
    }

    @Override
    public boolean isBlocking() {
        return this.blocking;
    }

    public void setBlocking(boolean blocking) {
        if (this.blocking != blocking) {
            this.blocking = blocking;
            if (blocking) {
                KeyBindingInputState.sendRightButtonDown();
            } else {
                KeyBindingInputState.sendRightButtonUp();
            }
        }
    }

    private boolean isReleaseUseItemPacket(Packet packet) {
        if (packet.isInstance(MappedClasses.DN)) {
            return new CPacketPlayerDigging(packet).isReleaseUseItem();
        }
        return false;
    }

    private void flushPackets() {
        EventPacketSend firstQueuedPacket = this.queuedPackets.peek();
        if (firstQueuedPacket == null) {
            return;
        }
        Queue<EventPacketSend> pendingPackets = new ConcurrentLinkedQueue<EventPacketSend>();
        EventPacketSend queuedPacket;
        while ((queuedPacket = this.queuedPackets.poll()) != null) {
            pendingPackets.add(queuedPacket);
        }
        PacketDispatchGuard.b(firstQueuedPacket.getNetworkManager(),
                () -> this.flushQueuedPackets(pendingPackets));
    }

    private void beginBuffering(EventPacketSend releaseEvent) {
        if (this.bufferingPackets) {
            if (releaseEvent != null) {
                this.queuedPackets.add(releaseEvent);
                releaseEvent.setCancelled(true);
            }
            return;
        }
        this.primaryActionClaim.markClaimed();
        if (releaseEvent != null) {
            this.queuedPackets.add(releaseEvent);
            releaseEvent.setCancelled(true);
        }
        this.bufferingPackets = true;
        this.delayMillis = (int)this.delay.getRandomValue();
        this.bufferTimer.reset();
    }

    private void stopBuffering(boolean flush) {
        if (flush) {
            this.flushPackets();
        } else {
            this.queuedPackets.clear();
        }
        this.bufferingPackets = false;
        this.setBlocking(false);
        this.blockCycleCompleted = false;
        this.primaryActionClaim.clearClaimed();
    }

    @Override
    public String getDetailedSuffix() {
        String displayText = "Lag " + this.delay.getDisplayValue() + "ms";
        if (this.bufferingPackets) {
            displayText = "\u00a7c" + displayText;
        }
        return displayText;
    }


    @EventHandler(priority=EventPriority.LOWEST)
    public void onPacketSend(EventPacketSend event) {
        if (event.wasModified()) {
            return;
        }
        Packet packet = event.getPacket();
        if (this.dispatchGuard.R(packet)) {
            return;
        }
        if (Minecraft.thePlayer().isNull() || packet.isInstance(MappedClasses.VP)) {
            return;
        }
        boolean releaseUseItem = this.isReleaseUseItemPacket(packet);
        if (event.isCanceled()) {
            if (!this.bufferingPackets && releaseUseItem && this.findTarget() != null
                    && this.isHoldingSword()) {
                this.beginBuffering(null);
            }
            return;
        }
        if (this.bufferingPackets) {
            boolean targetLost = this.findTarget() == null || !this.isHoldingSword();
            if (targetLost || this.bufferTimer.hasTimeElapsed(this.delayMillis)) {
                this.flushPackets();
                this.bufferingPackets = false;
                this.blockCycleCompleted = false;
                this.primaryActionClaim.clearClaimed();
            } else {
                this.queuedPackets.add(event);
                event.setCancelled(true);
            }
            return;
        }
        if (releaseUseItem && this.findTarget() != null && this.isHoldingSword()) {
            this.beginBuffering(event);
        }
        if (!this.bufferingPackets && this.queuedPackets.isEmpty()) {
            this.primaryActionClaim.clearClaimed();
            this.dispatchGuard.J(packet);
        }
    }

    public EntityLivingBase findTarget() {
        Animations parent = (Animations)this.getParent();
        return parent.findTarget((Double)parent.targetAngle.getValue(), (Double)parent.targetDistance.getValue());
    }

    private boolean isHoldingSword() {
        if (Minecraft.currentScreen().isNotNull()) {
            return false;
        }
        return Minecraft.thePlayer().getHeldItemHand().isNotNull() && ItemStackScoreUtil.h(Minecraft.thePlayer().getHeldItemHand().getItem());
    }

    private void flushQueuedPackets(Queue<EventPacketSend> pendingPackets) {
        for (EventPacketSend event : pendingPackets) {
            this.dispatchGuard.o(event);
        }
        pendingPackets.clear();
    }

    @Override
    public void onDisable() {
        this.stopBuffering(true);
    }

    public LegacyBlockingPacketBufferedAnimationsMode(Mod parent, String name) {
        super(parent, name);
        this.queuedPackets = new ConcurrentLinkedQueue<EventPacketSend>();
        this.delay = RandomValue.create(this, "Delay //$ui", "#", "", 0.0, 50.0, 100.0, 500.0);
        this.bufferTimer = new TimerUtil();
        this.primaryActionClaim = SharedModuleControlClaims.primaryAction;
        this.addValue(this.delay);
        this.primaryActionClaim.setPriority(this, 5);
    }
}
