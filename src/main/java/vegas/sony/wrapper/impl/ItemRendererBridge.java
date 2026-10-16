package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ItemRendererBridge
extends Wrapper {
    public static ItemRendererBridge firstPerson() {
        return new ItemRendererBridge(ItemRendererBridge.sonyVegasInstance.getMappingsMapperCompat().legacyItemCameraTransformType.getFirstPerson());
    }

    public ItemRendererBridge(Object handle) {
        super(handle);
    }
}
