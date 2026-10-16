package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEmptyItemAttributeModifiers;

public class EmptyItemAttributeModifiers
extends ItemAttributeModifiers {
    public static EmptyItemAttributeModifiers create() {
        return new EmptyItemAttributeModifiers(MEmptyItemAttributeModifiers.create(EmptyItemAttributeModifiers.sonyVegasInstance.getMappingsMapperCompat().Rw));
    }

    public EmptyItemAttributeModifiers(Object wrappedObject) {
        super(wrappedObject);
    }
}
