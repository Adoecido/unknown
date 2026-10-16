package vegas.sony.protocol.packet;

import vegas.sony.friend.FriendModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class ServerFriendModelPacket
implements ZeusSerializablePacket {
    private FriendModel friend;

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        this.friend.writeTo(zeusPacketBuffer);
    }

    public FriendModel getFriend() {
        return this.friend;
    }

    public ServerFriendModelPacket(FriendModel friend) {
        this.friend = friend;
    }

    public ServerFriendModelPacket() {
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.friend = new FriendModel(zeusPacketBuffer);
    }
}
