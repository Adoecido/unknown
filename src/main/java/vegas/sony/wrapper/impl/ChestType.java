package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ChestType
extends Wrapper {
    public ChestType(Object object) {
        super(object);
    }

    public static ChestType e() {
        return new ChestType(ChestType.sonyVegasInstance.getMappingsMapperCompat().qa.getFallDamageResetting());
    }
}
