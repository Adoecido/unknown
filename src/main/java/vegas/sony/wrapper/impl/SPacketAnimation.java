package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SPacketAnimation
extends Wrapper {
    public int getEntityId() {
        return SPacketAnimation.sonyVegasInstance.getMappingsMapperCompat().DF.getEntityId(this.I);
    }

    public int getAnimationType() {
        return SPacketAnimation.sonyVegasInstance.getMappingsMapperCompat().DF.getAnimationType(this.I);
    }

    public SPacketAnimation(Object handle) {
        super(handle);
    }
}
