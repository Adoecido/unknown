package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ResourceLocation
extends Wrapper {
    public static ResourceLocation create(String location) {
        return new ResourceLocation(ResourceLocation.sonyVegasInstance.getMappingsMapperCompat().resourceLocation.create(location));
    }

    public String getResourcePath() {
        return ResourceLocation.sonyVegasInstance.getMappingsMapperCompat().resourceLocation.getPath(this.I);
    }

    public ResourceLocation(Object resourceLocationHandle) {
        super(resourceLocationHandle);
    }
}
