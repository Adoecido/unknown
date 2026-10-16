package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MResourceKeyEnchantmentBridge;
import vegas.sony.wrapper.Wrapper;

public class ResourceKeyEnchantmentBridge
extends Wrapper {
    public static ResourceKey enchantment() {
        return new ResourceKey(MResourceKeyEnchantmentBridge.getEnchantment(ResourceKeyEnchantmentBridge.sonyVegasInstance.getMappingsMapperCompat().enchantmentRegistryKey));
    }

    public ResourceKeyEnchantmentBridge(Object handle) {
        super(handle);
    }
}
