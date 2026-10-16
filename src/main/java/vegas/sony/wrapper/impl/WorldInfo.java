package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class WorldInfo
extends Wrapper {
    public long getWorldTime() {
        return WorldInfo.sonyVegasInstance.getMappingsMapperCompat().worldInfo.getWorldTime(this.I);
    }

    public WorldInfo(Object wrappedObject) {
        super(wrappedObject);
    }
}
