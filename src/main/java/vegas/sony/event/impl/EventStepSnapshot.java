package vegas.sony.event.impl;

import vegas.sony.event.impl.EventStep;

public class EventStepSnapshot
extends EventStep {
    public EventStepSnapshot(Object entityHandle) {
        super(entityHandle);
        EventStep.storeOriginalStepHeight(this.getEntity().u());
    }
}
