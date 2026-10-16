package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SPacketChunkDataExtracted
extends Wrapper {
    public SPacketChunkDataExtracted(Object handle) {
        super(handle);
    }

    public int getDataSize() {
        return SPacketChunkDataExtracted.sonyVegasInstance.getMappings().b.getDataSize(this.I);
    }

    public byte[] getData() {
        return SPacketChunkDataExtracted.sonyVegasInstance.getMappings().b.getData(this.I);
    }
}
