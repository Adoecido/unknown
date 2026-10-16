package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;
import java.util.Collection;

public class ItemAttributeModifiers
extends Wrapper {
    public int size() {
        return ItemAttributeModifiers.sonyVegasInstance.getMappingsMapperCompat().itemAttributeModifiers.size(this.I);
    }

    public boolean put(Object key, Object value) {
        return ItemAttributeModifiers.sonyVegasInstance.getMappingsMapperCompat().itemAttributeModifiers.put(this.I, key, value);
    }

    public Collection values() {
        return ItemAttributeModifiers.sonyVegasInstance.getMappingsMapperCompat().itemAttributeModifiers.values(this.I);
    }

    public ItemAttributeModifiers(Object handle) {
        super(handle);
    }
}
