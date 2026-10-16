package vegas.sony.event;

import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.wrapper.impl.Packet;

public class PacketSendDispatchGuardCallback {
    private final EventPacketSend event;

    public PacketSendDispatchGuardCallback(EventPacketSend eventPacketSend) {
        this.event = eventPacketSend;
    }

    public void dispatch(PacketDispatchGuard dispatchGuard) {
        dispatchGuard.o(this.event);
    }

    public Packet getPacket() {
        return this.event.getPacket();
    }
}
