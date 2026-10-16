package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.GroupInvitePacket;
import vegas.sony.protocol.packet.GroupInviteStatus;
import vegas.sony.protocol.packet.ZeusTrackedPacket;
import vegas.sony.ui.click.component.GuiComponent;

public class GroupInviteResponsePacket
extends ZeusTrackedPacket<GroupInvitePacket> {
    private GroupInviteStatus status;
    private static GuiComponent[] S;

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
        this.status = zeusPacketBuffer.readEnum(GroupInviteStatus.class);
    }

    public GroupInviteStatus getStatus() {
        return this.status;
    }

    public GroupInviteResponsePacket() {
    }

    public static GuiComponent[] O() {
        return S;
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeEnum(this.status);
    }

    public static void E(GuiComponent[] guiComponentArray) {
        S = guiComponentArray;
    }

    public GroupInviteResponsePacket(GroupInvitePacket groupInvitePacket, GroupInviteStatus status) {
        super(groupInvitePacket);
        this.status = status;
    }

    static {
        if (GroupInviteResponsePacket.O() != null) {
            GroupInviteResponsePacket.E(new GuiComponent[4]);
        }
    }
}
