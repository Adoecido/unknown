package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;
import org.jetbrains.annotations.Nullable;

public class FriendServerAddressPacket
implements ZeusSerializablePacket {
    private long userId;
    private String serverAddress;

    public FriendServerAddressPacket(UserModel userModel, String serverAddress) {
        this.userId = userModel.getId();
        this.serverAddress = serverAddress;
    }

    public long getUserId() {
        return this.userId;
    }


    public FriendServerAddressPacket() {
    }

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeLong(this.userId);
        ZeusPacketBuffer zeusPacketBuffer2 = zeusPacketBuffer;
        boolean hasServerAddress = this.serverAddress != null;
        zeusPacketBuffer2.writeBoolean(hasServerAddress);
        if (this.serverAddress != null) {
            zeusPacketBuffer.writeString(this.serverAddress);
        }
    }

    @Nullable
    public String getServerAddress() {
        return this.serverAddress;
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.userId = zeusPacketBuffer.readLong();
        if (zeusPacketBuffer.readBoolean()) {
            this.serverAddress = zeusPacketBuffer.readString(128);
        }
    }
}
