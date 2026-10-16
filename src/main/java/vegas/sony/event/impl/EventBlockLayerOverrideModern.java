package vegas.sony.event.impl;

import vegas.sony.event.impl.EventBlockLayerOverride;

public class EventBlockLayerOverrideModern
extends EventBlockLayerOverride {
    @Override
    public boolean fire() {
        return super.fire();
    }

    public EventBlockLayerOverrideModern(Object blockHandle) {
        super(blockHandle);
    }
}
