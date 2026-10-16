package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.FriendDeleteResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class FriendDeletePacket
extends ZeusTrackedPacket<FriendDeleteResponsePacket> {
    private UserModel user;

    public FriendDeletePacket() {
    }

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
        this.user = new UserModel(zeusPacketBuffer);
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
        this.user.writeTo(zeusPacketBuffer);
    }

    public FriendDeletePacket(UserModel user) {
        this.user = user;
    }

    public UserModel getUser() {
        return this.user;
    }
}
