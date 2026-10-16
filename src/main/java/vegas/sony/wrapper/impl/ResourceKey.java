package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MResourceKey;
import vegas.sony.wrapper.Wrapper;

public class ResourceKey
extends Wrapper {
    public ResourceKey(Object wrappedObject) {
        super(wrappedObject);
    }

    public ResourceLocation getLocation() {
        return new ResourceLocation(MResourceKey.getLocation(ResourceKey.sonyVegasInstance.getMappingsMapperCompat().P, this.I));
    }
}
