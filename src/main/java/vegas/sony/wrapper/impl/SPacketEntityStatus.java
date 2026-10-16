package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SPacketEntityStatus
extends Wrapper {
    public byte getLogicOpcode() {
        return SPacketEntityStatus.sonyVegasInstance.getMappingsMapperCompat().RO.getLogicOpcode(this.I);
    }

    public int getEntityId() {
        return SPacketEntityStatus.sonyVegasInstance.getMappingsMapperCompat().RO.getEntityId(this.I);
    }

    public SPacketEntityStatus(Object handle) {
        super(handle);
    }
}
