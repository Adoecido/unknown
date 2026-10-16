package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ChestTypeHolder
extends Wrapper {
    public static ItemCameraTransformSubtypeValue woodenToolMaterials() {
        return new ItemCameraTransformSubtypeValue(ChestTypeHolder.sonyVegasInstance.getMappingsMapperCompat().L.getWoodenToolMaterials());
    }

    public static ItemCameraTransformSubtypeValue goldToolMaterials() {
        return new ItemCameraTransformSubtypeValue(ChestTypeHolder.sonyVegasInstance.getMappingsMapperCompat().L.getGoldToolMaterials());
    }

    public ChestTypeHolder(Object wrappedObject) {
        super(wrappedObject);
    }
}
