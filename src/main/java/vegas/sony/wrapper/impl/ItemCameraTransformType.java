package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ItemCameraTransformType
extends Wrapper {
    public ItemCameraTransformBase getItems() {
        return new ItemCameraTransformBase(ItemCameraTransformType.sonyVegasInstance.getMappingsMapperCompat().repairable.getItems(this.I));
    }

    public ItemCameraTransformType(Object handle) {
        super(handle);
    }
}
