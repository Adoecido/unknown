package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class ServerFriendRequestRemovedPacket
implements ZeusSerializablePacket {
    private long userId;

    public long getUserId() {
        return this.userId;
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.userId = zeusPacketBuffer.readLong();
    }

    public ServerFriendRequestRemovedPacket() {
    }

    public ServerFriendRequestRemovedPacket(long userId) {
        this.userId = userId;
    }

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeLong(this.userId);
    }
}
