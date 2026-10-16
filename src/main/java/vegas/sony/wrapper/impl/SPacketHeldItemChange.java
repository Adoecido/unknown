package vegas.sony.wrapper.impl;

public class SPacketHeldItemChange
extends Packet {
    public SPacketHeldItemChange(Object handle) {
        super(handle);
    }

    public static SPacketHeldItemChange createCloseWindowPacket(int windowId) {
        return new SPacketHeldItemChange(SPacketHeldItemChange.sonyVegasInstance.getMappingsMapperCompat().C6.createCloseWindowPacket(windowId));
    }
}
