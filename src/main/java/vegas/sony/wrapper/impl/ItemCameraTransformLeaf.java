package vegas.sony.wrapper.impl;

public class ItemCameraTransformLeaf
extends ItemCameraTransformIntermediate {
    public ItemCameraTransformSubtypeValue getKey() {
        return new ItemCameraTransformSubtypeValue(ItemCameraTransformLeaf.sonyVegasInstance.getMappingsMapperCompat().C.getKey(this.I));
    }

    public ItemCameraTransformLeaf(Object wrappedObject) {
        super(wrappedObject);
    }
}
