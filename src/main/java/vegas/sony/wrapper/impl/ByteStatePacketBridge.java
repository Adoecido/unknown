package vegas.sony.wrapper.impl;

public class ByteStatePacketBridge
extends Packet {
    public ByteStatePacketBridge(Object handle) {
        super(handle);
    }

    public byte getHeadYaw() {
        return ByteStatePacketBridge.sonyVegasInstance.getMappingsMapperCompat().hy.getHeadYaw(this.I);
    }
}
