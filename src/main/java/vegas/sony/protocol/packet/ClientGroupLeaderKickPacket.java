package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ClientGroupLeaderKickResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class ClientGroupLeaderKickPacket
extends ZeusTrackedPacket<ClientGroupLeaderKickResponsePacket> {
    private long f;

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeLong(this.f);
    }

    public ClientGroupLeaderKickPacket() {
    }

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
        this.f = zeusPacketBuffer.readLong();
    }

    public long H() {
        return this.f;
    }

    public ClientGroupLeaderKickPacket(UserModel userModel) {
        this.f = userModel.getId();
    }
}

