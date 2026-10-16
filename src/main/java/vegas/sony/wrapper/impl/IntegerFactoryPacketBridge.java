package vegas.sony.wrapper.impl;

public class IntegerFactoryPacketBridge
extends Packet {
    public static IntegerFactoryPacketBridge createConfirmTeleportPacket(int teleportId) {
        return new IntegerFactoryPacketBridge(IntegerFactoryPacketBridge.sonyVegasInstance.getMappingsMapperCompat().hp.createConfirmTeleportPacket(teleportId));
    }

    public IntegerFactoryPacketBridge(Object handle) {
        super(handle);
    }
}
