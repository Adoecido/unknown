package vegas.sony.wrapper.impl;

public class CPacketHeldItemChange
extends Packet {
    public static CPacketHeldItemChange create(int selectedSlot) {
        return new CPacketHeldItemChange(CPacketHeldItemChange.sonyVegasInstance.getMappingsMapperCompat().Ck.createPacket(selectedSlot));
    }

    public CPacketHeldItemChange(Object handle) {
        super(handle);
    }
}
