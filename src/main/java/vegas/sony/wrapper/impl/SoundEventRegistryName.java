package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MSoundEventRegistryName;
import vegas.sony.wrapper.Wrapper;

public class SoundEventRegistryName
extends Wrapper {
    public ResourceLocation getRegistryName() {
        return new ResourceLocation(MSoundEventRegistryName.getName(SoundEventRegistryName.sonyVegasInstance.getMappingsMapperCompat().h9, this.I));
    }

    public SoundEventRegistryName(Object wrappedObject) {
        super(wrappedObject);
    }
}
