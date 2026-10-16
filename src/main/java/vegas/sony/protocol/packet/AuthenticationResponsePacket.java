package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.AuthenticationPacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class AuthenticationResponsePacket
extends ZeusTrackedPacket<AuthenticationPacket> {
    private UserModel E;

    @Override
    public void x(ZeusPacketBuffer gx_12) {
        int n = gx_12.readVarInt();
        this.E = new UserModel(gx_12);
    }

    public AuthenticationResponsePacket() {
    }

    public UserModel s() {
        return this.E;
    }

    @Override
    public void T(ZeusPacketBuffer gx_12) {
        gx_12.writeVarInt(1);
        this.E.writeTo(gx_12);
    }

    public AuthenticationResponsePacket(AuthenticationPacket authenticationPacket, UserModel oj_12) {
        this(authenticationPacket);
        this.E = oj_12;
    }

    public AuthenticationResponsePacket(AuthenticationPacket authenticationPacket) {
        super(authenticationPacket);
    }
}

