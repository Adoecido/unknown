package vegas.sony.event.impl;

import vegas.sony.event.impl.EventBlockLayerOverride;

public class EventBlockLayerOverrideLegacy
extends EventBlockLayerOverride {
    @Override
    public boolean fire() {
        return super.fire();
    }

    public EventBlockLayerOverrideLegacy(Object blockHandle) {
        super(blockHandle);
    }
}
