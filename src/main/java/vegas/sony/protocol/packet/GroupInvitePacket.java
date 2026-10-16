package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.GroupInviteResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class GroupInvitePacket
extends ZeusTrackedPacket<GroupInviteResponsePacket> {
    private UserModel invitedUser;

    public UserModel getInvitedUser() {
        return this.invitedUser;
    }

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
        this.invitedUser = new UserModel(zeusPacketBuffer);
    }

    public GroupInvitePacket() {
    }

    public GroupInvitePacket(UserModel invitedUser) {
        this.invitedUser = invitedUser;
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
        this.invitedUser.writeTo(zeusPacketBuffer);
    }
}
