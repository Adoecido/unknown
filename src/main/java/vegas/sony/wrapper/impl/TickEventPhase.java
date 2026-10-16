package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTickEventPhase;
import vegas.sony.wrapper.Wrapper;

public class TickEventPhase
extends Wrapper {
    public static TickEventPhase q() {
        return new TickEventPhase(MTickEventPhase.B(TickEventPhase.sonyVegasInstance.getMappingsMapperCompat().R8));
    }

    public TickEventPhase(Object object) {
        super(object);
    }

    public static TickEventPhase J() {
        return new TickEventPhase(MTickEventPhase.G(TickEventPhase.sonyVegasInstance.getMappingsMapperCompat().R8));
    }
}

