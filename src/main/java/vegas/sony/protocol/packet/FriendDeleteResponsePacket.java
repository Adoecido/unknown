package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.FriendDeletePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;
import org.jetbrains.annotations.Nullable;

public class FriendDeleteResponsePacket
extends ZeusTrackedPacket<FriendDeletePacket> {
    private boolean deleted;

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
        this.deleted = zeusPacketBuffer.readBoolean();
    }

    public FriendDeleteResponsePacket(@Nullable FriendDeletePacket friendDeletePacket, boolean deleted) {
        super(friendDeletePacket);
        this.deleted = deleted;
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeBoolean(this.deleted);
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public FriendDeleteResponsePacket() {
    }
}
