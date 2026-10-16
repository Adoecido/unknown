package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class PlaySoundAtEntityEvent
extends Wrapper {
    public PlaySoundAtEntityEvent(Object object) {
        super(object);
    }

    public String getName() {
        return PlaySoundAtEntityEvent.sonyVegasInstance.getMappingsMapperCompat().C5.Z(this.I);
    }
}

