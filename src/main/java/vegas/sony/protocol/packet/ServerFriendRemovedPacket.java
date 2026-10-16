package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class ServerFriendRemovedPacket
implements ZeusSerializablePacket {
    private UserModel M;

    public UserModel m() {
        return this.M;
    }

    public ServerFriendRemovedPacket() {
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.M = new UserModel(zeusPacketBuffer);
    }

    public ServerFriendRemovedPacket(UserModel userModel) {
        this.M = userModel;
    }

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        this.M.writeTo(zeusPacketBuffer);
    }
}

