package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class OpenedChestTypeSentinel
extends Wrapper {
    public OpenedChestTypeSentinel(Object wrappedObject) {
        super(wrappedObject);
    }

    public static OpenedChestTypeSentinel basic() {
        return new OpenedChestTypeSentinel(OpenedChestTypeSentinel.sonyVegasInstance.getMappingsMapperCompat().CD.getBasic());
    }
}
