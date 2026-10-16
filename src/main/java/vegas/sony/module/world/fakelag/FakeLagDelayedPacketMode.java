package vegas.sony.module.world.fakelag;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.world.FakeLag;
import vegas.sony.utils.SleepUtil;
import vegas.sony.utils.network.PacketDispatchTask;
import vegas.sony.utils.network.TimedPacketDispatchTask;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FakeLagDelayedPacketMode
extends SubModule<FakeLag> {
    private final Queue<TimedPacketDispatchTask> pendingPackets = new ConcurrentLinkedQueue<TimedPacketDispatchTask>();
    private boolean flushing = false;

    @Override
    public void onBeforeDisable() {
        this.flushing = true;
        this.flush(true);
    }

    private void flush(boolean force) {
        TimedPacketDispatchTask pendingPacket;
        while ((pendingPacket = this.pendingPackets.peek()) != null && (pendingPacket.j(((Double)((FakeLag)this.getParent()).delay.getValue()).longValue()) || force)) {
            TimedPacketDispatchTask dispatched = this.pendingPackets.poll();
            Packet packet = dispatched.z().s();
            dispatched.z().t();
            ((FakeLag)this.getParent()).recordServerPosition(packet);
        }
    }


    public FakeLagDelayedPacketMode(Mod parent, String name) {
        super(parent, name);
    }

    @Override
    public String getDetailedSuffix() {
        return "Latency" + ((FakeLag)this.getParent()).delay.getDisplayValue() + "ms";
    }

    @Override
    public void onDisable() {
        this.flushing = false;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPacketSend(EventPacketSend eventPacketSend) {
        if (Minecraft.thePlayer().isNull() || Minecraft.theWorld().isNull()) {
            this.flush(false);
            return;
        }
        if (eventPacketSend.isCanceled()) {
            this.flush(true);
            return;
        }
        if (this.flushing) {
            while (this.flushing) {
                SleepUtil.sleep(50L);
            }
            return;
        }
        this.flush(false);
        this.pendingPackets.add(new TimedPacketDispatchTask(new PacketDispatchTask(eventPacketSend.getPacket(), true, eventPacketSend.getNetworkManager()), null));
        eventPacketSend.setCancelled(true);
    }
}
