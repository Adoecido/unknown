package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class ServerGroupLeaderChangedPacket
implements ZeusSerializablePacket {
    private UserModel newLeader;

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        this.newLeader.writeTo(zeusPacketBuffer);
    }

    public ServerGroupLeaderChangedPacket(UserModel newLeader) {
        this.newLeader = newLeader;
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.newLeader = new UserModel(zeusPacketBuffer);
    }

    public UserModel getNewLeader() {
        return this.newLeader;
    }

    public ServerGroupLeaderChangedPacket() {
    }
}
