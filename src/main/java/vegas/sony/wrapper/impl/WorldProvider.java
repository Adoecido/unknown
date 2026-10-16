package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class WorldProvider
extends Wrapper {
    public boolean hasNoSky() {
        return WorldProvider.sonyVegasInstance.getMappingsMapperCompat().worldProvider.hasNoSky(this.I);
    }

    public WorldProvider(Object wrappedObject) {
        super(wrappedObject);
    }
}
