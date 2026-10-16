package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SoundEvent
extends Wrapper {
    public SoundEvent(Object object) {
        super(object);
    }

    public ResourceLocation V() {
        return new ResourceLocation(SoundEvent.sonyVegasInstance.getMappingsMapperCompat().Cb.getName(this.I));
    }
}
