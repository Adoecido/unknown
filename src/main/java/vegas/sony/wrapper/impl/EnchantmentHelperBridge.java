package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class EnchantmentHelperBridge
extends Wrapper {
    public EnchantmentHelperBridge(Object handle) {
        super(handle);
    }

    public static EnchantmentRegistryAccess createLookup() {
        return new EnchantmentRegistryAccess(EnchantmentHelperBridge.sonyVegasInstance.getMappingsMapperCompat().vanillaRegistries.createLookup());
    }
}
