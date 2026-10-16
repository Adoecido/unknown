package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class RegistryAccess
extends Wrapper {
    public RegistryAccess(Object wrappedObject) {
        super(wrappedObject);
    }

    public Registry lookupOrThrow(ResourceKey resourceKey) {
        return new Registry(RegistryAccess.sonyVegasInstance.getMappingsMapperCompat().qs.lookupOrThrow(this.I, resourceKey.getObject()));
    }
}
