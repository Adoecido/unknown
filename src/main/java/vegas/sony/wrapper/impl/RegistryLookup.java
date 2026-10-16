package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

import java.util.Optional;

public class RegistryLookup
extends Wrapper {
    public Optional<Object> get(ResourceKey resourceKey) {
        return RegistryLookup.sonyVegasInstance.getMappingsMapperCompat().holderGetterProvider.get(this.getObject(), resourceKey.getObject());
    }

    public Optional<Object> lookup(ResourceKey resourceKey) {
        return RegistryLookup.sonyVegasInstance.getMappingsMapperCompat().holderGetterProvider.lookup(this.getObject(), resourceKey.getObject());
    }

    public Object lookupOrThrow(ResourceKey resourceKey) {
        return RegistryLookup.sonyVegasInstance.getMappingsMapperCompat().holderGetterProvider.lookupOrThrow(this.getObject(), resourceKey.getObject());
    }

    public RegistryLookup(Object handle) {
        super(handle);
    }
}
