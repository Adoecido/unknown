package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.GroupInviteStateResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class GroupInviteStatePacket
extends ZeusTrackedPacket<GroupInviteStateResponsePacket> {
    private boolean acceptInvite;
    private long userId;

    public GroupInviteStatePacket() {
    }

    public long getUserId() {
        return this.userId;
    }

    public boolean isAcceptInvite() {
        return this.acceptInvite;
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeLong(this.userId);
        zeusPacketBuffer.writeBoolean(this.acceptInvite);
    }

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
        this.userId = zeusPacketBuffer.readLong();
        this.acceptInvite = zeusPacketBuffer.readBoolean();
    }

    public GroupInviteStatePacket(UserModel userModel, boolean acceptInvite) {
        this.userId = userModel.getId();
        this.acceptInvite = acceptInvite;
    }
}
