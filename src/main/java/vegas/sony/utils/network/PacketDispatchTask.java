package vegas.sony.utils.network;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.utils.network.PacketDispatchMarkerRegistry;
import vegas.sony.wrapper.impl.NetworkManager;
import vegas.sony.wrapper.impl.NetworkPacketHandle;
import vegas.sony.wrapper.impl.Packet;

public class PacketDispatchTask {
    private Packet n;
    private NetworkManager h;
    private boolean N;

    public void t() {
        block6: {
            if (this.A()) {
                if (PacketDispatchMarkerRegistry.S(this.n)) {
                    PacketDispatchMarkerRegistry.I(this.n);
                    return;
                }
                PacketDispatchMarkerRegistry.u(this.n);
                NetworkPacketHandle networkPacketHandle = this.h.c();
                if (!networkPacketHandle.isInstance(MappedClasses.F1)) {
                    return;
                }
                this.h.G(this.n);
            } else {
                try {
                    this.n.processPacket(this.h.c());
                }
                catch (Exception exception) {
                    if (!this.A()) break block6;
                    SonyVegas.logThrowable(exception);
                }
            }
        }
    }

    public boolean A() {
        return this.N;
    }

    public Packet s() {
        return this.n;
    }

    private static Exception a(Exception exception) {
        return exception;
    }

    public boolean equals(Object object) {
        if (object instanceof PacketDispatchTask) {
            PacketDispatchTask packetDispatchTask = (PacketDispatchTask)object;
            return packetDispatchTask.s().equals(this.n) && packetDispatchTask.A() == this.N;
        }
        if (object instanceof Packet) {
            return this.n.equals(object);
        }
        return super.equals(object);
    }

    public PacketDispatchTask(Packet packet, boolean bl, NetworkManager networkManager) {
        this.n = packet;
        this.N = bl;
        this.h = networkManager;
    }
}

