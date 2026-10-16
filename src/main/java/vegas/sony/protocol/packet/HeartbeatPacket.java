package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.HeartbeatResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class HeartbeatPacket
extends ZeusTrackedPacket<HeartbeatResponsePacket> {
    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
    }
}

