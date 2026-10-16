package vegas.sony.module.rise.velocity;

import java.util.ArrayDeque;
import java.util.Deque;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.rise.RiseVelocity;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.NetHandlerPlayClientImpl;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

public final class RiseVelocityDelayMode extends SubModule<RiseVelocity> {
    private final NumberValue delayTicks = NumberValue.create(this, "Delay (ticks) //$ui", "#", "",
            0.0, 5.0, 40.0, 1.0);
    private final BooleanValue delayExplosions = BooleanValue.create(this, "Delay Explosions //$ui", true);
    private final Deque<DelayedPacket> delayedPackets = new ArrayDeque<DelayedPacket>();
    private final PacketDispatchGuard dispatchGuard = PacketDispatchGuard.b;

    public RiseVelocityDelayMode(Mod parent, String name) {
        super(parent, name);
        this.delayTicks.setMaximumFractionDigits(0);
        this.addValue(this.delayTicks, this.delayExplosions);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPacketReceive(EventPacketReceive event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        Packet packet = event.getPacket();
        if (this.dispatchGuard.R(packet)) {
            return;
        }
        boolean[] delay = new boolean[]{false};
        Packet.n(packet, nested -> {
            if (this.shouldDelay(nested, player)) {
                delay[0] = true;
            }
        });
        if (!delay[0]) {
            this.dispatchGuard.J(packet);
            return;
        }
        event.setCancelled(true);
        synchronized (this.delayedPackets) {
            this.delayedPackets.addLast(new DelayedPacket(packet,
                    player.l() + this.delayTicks.getValue().intValue()));
        }
    }

    private boolean shouldDelay(Packet packet, EntityPlayerSP player) {
        if (packet.isInstance(MappedClasses.YX)) {
            return new SPacketEntityVelocity(packet.getObject()).getEntityId() == player.S();
        }
        if (MappedClasses.YC != null && packet.isInstance(MappedClasses.YC)) {
            return true;
        }
        return this.delayExplosions.getEffectiveValue().booleanValue()
                && packet.isInstance(MappedClasses.qe);
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        int currentTick = player.l();
        PacketDispatchGuard.B(() -> this.replayDue(currentTick, player.sendQueue()));
    }

    private void replayDue(int currentTick, NetHandlerPlayClientImpl handler) {
        synchronized (this.delayedPackets) {
            while (!this.delayedPackets.isEmpty()
                    && this.delayedPackets.peekFirst().replayTick <= currentTick) {
                this.dispatchGuard.l(this.delayedPackets.removeFirst().packet, handler);
            }
        }
    }

    private void replayAll() {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            synchronized (this.delayedPackets) {
                while (!this.delayedPackets.isEmpty()) {
                    this.dispatchGuard.J(this.delayedPackets.removeFirst().packet);
                }
            }
            return;
        }
        NetHandlerPlayClientImpl handler = player.sendQueue();
        synchronized (this.delayedPackets) {
            while (!this.delayedPackets.isEmpty()) {
                this.dispatchGuard.l(this.delayedPackets.removeFirst().packet, handler);
            }
        }
    }

    @Override
    public void onDisable() {
        PacketDispatchGuard.B(this::replayAll);
    }

    @Override
    public String getDetailedSuffix() {
        return this.delayTicks.getDisplayValue() + "t";
    }

    private static final class DelayedPacket {
        private final Packet packet;
        private final int replayTick;

        private DelayedPacket(Packet packet, int replayTick) {
            this.packet = packet;
            this.replayTick = replayTick;
        }
    }
}
