package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ClientGroupLeaderPromoteResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class ClientGroupLeaderPromotePacket
extends ZeusTrackedPacket<ClientGroupLeaderPromoteResponsePacket> {
    private long U;

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
        this.U = zeusPacketBuffer.readLong();
    }

    public ClientGroupLeaderPromotePacket(UserModel userModel) {
        this.U = userModel.getId();
    }

    public ClientGroupLeaderPromotePacket() {
    }

    public long d() {
        return this.U;
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeLong(this.U);
    }
}

