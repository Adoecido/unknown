package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MNetHandlerPlayClient;

public class NetHandlerPlayClient
extends NetworkPacketHandle {
    public void handleEntityTeleport(SPacketEntity teleportPacket) {
        MNetHandlerPlayClient.handleEntityTeleport(NetHandlerPlayClient.sonyVegasInstance.getMappingsMapperCompat().netHandlerPlayClient, this.I, teleportPacket.getObject());
    }

    public NetHandlerPlayClient(Object handle) {
        super(handle);
    }
}
