package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.FriendsListResponsePacket;
import vegas.sony.protocol.packet.ZeusTrackedPacket;

public class FriendsListPacket
extends ZeusTrackedPacket<FriendsListResponsePacket> {
    private static boolean K;

    public static boolean s() {
        return K;
    }

    @Override
    public void x(ZeusPacketBuffer zeusPacketBuffer) {
    }

    public static void m(boolean bl) {
        K = bl;
    }

    @Override
    public void T(ZeusPacketBuffer zeusPacketBuffer) {
    }


    public static boolean e() {
        boolean bl = FriendsListPacket.s();
        return !bl;
    }

    static {
        if (FriendsListPacket.e()) {
            FriendsListPacket.m(true);
        }
    }
}

