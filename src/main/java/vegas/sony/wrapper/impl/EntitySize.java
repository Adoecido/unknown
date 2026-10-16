package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class EntitySize
extends Wrapper {
    public float c() {
        return EntitySize.sonyVegasInstance.getMappingsMapperCompat().h2.getWidth(this.I);
    }

    public float u() {
        return EntitySize.sonyVegasInstance.getMappingsMapperCompat().h2.getHeight(this.I);
    }

    public EntitySize(Object object) {
        super(object);
    }
}

