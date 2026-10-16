package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ResourceKeyHolder
extends Wrapper {
    public static ResourceKey gold() {
        return new ResourceKey(ResourceKeyHolder.sonyVegasInstance.getMappingsMapperCompat().qV.getGold());
    }

    public ResourceKeyHolder(Object wrappedObject) {
        super(wrappedObject);
    }
}
