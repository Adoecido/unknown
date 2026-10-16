package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class ServerGroupInvitePacket
implements ZeusSerializablePacket {
    private UserModel H;

    public UserModel Z() {
        return this.H;
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.H = new UserModel(zeusPacketBuffer);
    }

    public ServerGroupInvitePacket(UserModel userModel) {
        this.H = userModel;
    }

    public ServerGroupInvitePacket() {
    }

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        this.H.writeTo(zeusPacketBuffer);
    }
}

