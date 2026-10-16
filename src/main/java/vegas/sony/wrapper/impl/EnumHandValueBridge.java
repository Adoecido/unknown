package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEnumHandBridge;
import vegas.sony.wrapper.Wrapper;

public class EnumHandValueBridge
extends Wrapper {
    public static EnumHandValueBridge v() {
        return new EnumHandValueBridge(MEnumHandBridge.c(EnumHandValueBridge.sonyVegasInstance.getMappingsMapperCompat().hR));
    }

    public EnumHandValueBridge(Object object) {
        super(object);
    }
}

