package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MBiomeProviderBridge;
import vegas.sony.wrapper.Wrapper;

public class BiomeProvider
extends Wrapper {
    public Iterable getAllEffects() {
        return MBiomeProviderBridge.getAllEffects(BiomeProvider.sonyVegasInstance.getMappingsMapperCompat().K, this.I);
    }

    public BiomeProvider(Object wrappedObject) {
        super(wrappedObject);
    }
}
