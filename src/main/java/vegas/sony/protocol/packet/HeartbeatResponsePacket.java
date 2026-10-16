package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.HeartbeatPacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;
import org.jetbrains.annotations.Nullable;

public class HeartbeatResponsePacket
extends ZeusTrackedPacket<HeartbeatPacket> {
    @Override
    public void T(ZeusPacketBuffer gx_12) {
    }

    public HeartbeatResponsePacket(@Nullable HeartbeatPacket tW) {
        super(tW);
    }

    @Override
    public void x(ZeusPacketBuffer gx_12) {
    }

    public HeartbeatResponsePacket() {
    }
}

