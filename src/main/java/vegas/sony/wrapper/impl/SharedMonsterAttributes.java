package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SharedMonsterAttributes
extends Wrapper {
    public SharedMonsterAttributes(Object object) {
        super(object);
    }

    public static SharedMonsterAttributes V() {
        return new SharedMonsterAttributes(SharedMonsterAttributes.sonyVegasInstance.getMappingsMapperCompat().h3.getSeeThrough());
    }

    public static SharedMonsterAttributes c() {
        return new SharedMonsterAttributes(SharedMonsterAttributes.sonyVegasInstance.getMappingsMapperCompat().h3.getNormal());
    }
}

