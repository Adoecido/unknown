package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MBiomeRegistryName;
import vegas.sony.wrapper.Wrapper;

public class BiomeRegistryName
extends Wrapper {
    public String n() {
        return MBiomeRegistryName.getName(BiomeRegistryName.sonyVegasInstance.getMappingsMapperCompat().hq, this.I);
    }

    public BiomeRegistryName(Object object) {
        super(object);
    }
}
