package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.SonyVegas;

public class EventMouseOverUpdate
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public EventMouseOverUpdate(float f) {
    }

    @Override
    public boolean fire() {
        // Injection callbacks may arrive before the mapper has been installed.
        if (SonyVegas.INSTANCE == null || SonyVegas.INSTANCE.getMappings() == null
                || SonyVegas.INSTANCE.getMappings().U == null) return false;
        return super.fire();
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }
}
