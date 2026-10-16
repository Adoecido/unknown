package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SharedMonsterAttributesVariantBridge
extends Wrapper {
    public SharedMonsterAttributesVariantBridge(Object object) {
        super(object);
    }

    public static SharedMonsterAttributesVariantBridge C() {
        return new SharedMonsterAttributesVariantBridge(SharedMonsterAttributesVariantBridge.sonyVegasInstance.getMappingsMapperCompat().D7.getAll());
    }

    public static SharedMonsterAttributesVariantBridge l() {
        return new SharedMonsterAttributesVariantBridge(SharedMonsterAttributesVariantBridge.sonyVegasInstance.getMappingsMapperCompat().D7.getNone());
    }
}
