package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class ServerGroupInviteRemovedPacket
implements ZeusSerializablePacket {
    private UserModel d;

    public ServerGroupInviteRemovedPacket() {
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.d = new UserModel(zeusPacketBuffer);
    }

    public ServerGroupInviteRemovedPacket(UserModel userModel) {
        this.d = userModel;
    }

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        this.d.writeTo(zeusPacketBuffer);
    }

    public UserModel G() {
        return this.d;
    }
}

