package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ResourceLocationKey
extends Wrapper {
    public static ResourceLocationKey L() {
        return new ResourceLocationKey(ResourceLocationKey.sonyVegasInstance.getMappingsMapperCompat().qK.r());
    }

    public ResourceLocationKey(Object object) {
        super(object);
    }
}

