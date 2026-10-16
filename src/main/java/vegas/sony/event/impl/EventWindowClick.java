package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;

public class EventWindowClick
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public EventWindowClick(Object ignoredWindowHandle) {
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }
}
