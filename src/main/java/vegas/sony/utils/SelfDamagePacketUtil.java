package vegas.sony.utils;

import vegas.sony.config.ClientSettings;
import vegas.sony.wrapper.impl.CPacketPlayerPosition;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public class SelfDamagePacketUtil {

    private void sendSelfDamagePackets() {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (!player.b$src$Z$fqlxe4() || !player.u$src$Z$g120nz()) {
            return;
        }
        for (int packetPair = 0; packetPair < 60; ++packetPair) {
            double highOffset = 0.13029834580989086 + 7.045809890852092E-4 * Math.random();
            CPacketPlayerPosition highPositionPacket = ClientSettings.IS_LEGACY_1_7
                    ? CPacketPlayerPosition.newInstance(player.z(), player.N() + highOffset, player.h(), false)
                    : CPacketPlayerPosition.newInstance(player.z(),
                            player.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY() + highOffset,
                            player.N() + highOffset, player.h(), false);
            double lowOffset = 0.07029834580989085 + 7.045809890852092E-4 * Math.random();
            CPacketPlayerPosition lowPositionPacket = ClientSettings.IS_LEGACY_1_7
                    ? CPacketPlayerPosition.newInstance(player.z(), player.N() + highOffset, player.h(), false)
                    : CPacketPlayerPosition.newInstance(player.z(),
                            player.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY() + lowOffset,
                            player.N() + lowOffset, player.h(), false);
            player.sendQueue().addToSendQueue(highPositionPacket);
            player.sendQueue().addToSendQueue(lowPositionPacket);
        }
        CPacketPlayerPosition landingPacket = ClientSettings.IS_LEGACY_1_7
                ? CPacketPlayerPosition.newInstance(player.z(), player.N(), player.h(), true)
                : CPacketPlayerPosition.newInstance(player.z(),
                        player.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY(),
                        player.N(), player.h(), true);
        player.sendQueue().addToSendQueue(landingPacket);
    }
}

