package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.GroupLeaveResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class GroupLeavePacket
extends ZeusTrackedPacket<GroupLeaveResponsePacket> {
    private static int[] z;

    @Override
    public void x(ZeusPacketBuffer gx_12) {
    }

    public static void b(int[] nArray) {
        z = nArray;
    }

    @Override
    public void T(ZeusPacketBuffer gx_12) {
    }

    public static int[] l() {
        return z;
    }

    static {
        if (GroupLeavePacket.l() != null) {
            GroupLeavePacket.b(new int[4]);
        }
    }
}

