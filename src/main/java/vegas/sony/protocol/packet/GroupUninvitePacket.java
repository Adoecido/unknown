package vegas.sony.protocol.packet;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.GroupUninviteResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class GroupUninvitePacket
extends ZeusTrackedPacket<GroupUninviteResponsePacket> {
    private static boolean u;
    private UserModel uninvitedUser;

    public GroupUninvitePacket() {
    }

    public UserModel getUninvitedUser() {
        return this.uninvitedUser;
    }

    public static void G(boolean bl) {
        u = bl;
    }

    @Override
    public void x(ZeusPacketBuffer gx_12) {
        this.uninvitedUser = new UserModel(gx_12);
    }


    public GroupUninvitePacket(UserModel uninvitedUser) {
        this.uninvitedUser = uninvitedUser;
    }

    public static boolean t() {
        return u;
    }

    @Override
    public void T(ZeusPacketBuffer gx_12) {
        this.uninvitedUser.writeTo(gx_12);
    }

    public static boolean U() {
        boolean bl = GroupUninvitePacket.t();
        return true;
    }

    static {
        if (GroupUninvitePacket.t()) {
            GroupUninvitePacket.G(true);
        }
    }
}
