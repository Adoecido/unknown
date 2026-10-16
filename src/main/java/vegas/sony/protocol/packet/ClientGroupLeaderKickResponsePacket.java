package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ClientGroupLeaderKickPacket;
import vegas.sony.protocol.packet.ClientGroupLeaderKickStatus;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class ClientGroupLeaderKickResponsePacket
extends ZeusTrackedPacket<ClientGroupLeaderKickPacket> {
    private ClientGroupLeaderKickStatus K;

    @Override
    public void x(ZeusPacketBuffer gx_12) {
        this.K = gx_12.readEnum(ClientGroupLeaderKickStatus.class);
    }

    @Override
    public void T(ZeusPacketBuffer gx_12) {
        gx_12.writeEnum(this.K);
    }

    public ClientGroupLeaderKickResponsePacket() {
    }

    public ClientGroupLeaderKickResponsePacket(ClientGroupLeaderKickPacket gq_02, ClientGroupLeaderKickStatus iH) {
        super(gq_02);
        this.K = iH;
    }

    public ClientGroupLeaderKickStatus P() {
        return this.K;
    }
}

