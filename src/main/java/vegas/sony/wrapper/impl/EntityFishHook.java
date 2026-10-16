package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class EntityFishHook
extends Wrapper {
    public EntityFishHook(Object object) {
        super(object);
    }

    public boolean o() {
        return EntityFishHook.sonyVegasInstance.getMappingsMapperCompat().Cr.isEmpty(this.I);
    }

    public AxisAlignedBB n() {
        return new AxisAlignedBB(EntityFishHook.sonyVegasInstance.getMappingsMapperCompat().Cr.getBoundingBox(this.I));
    }
}

