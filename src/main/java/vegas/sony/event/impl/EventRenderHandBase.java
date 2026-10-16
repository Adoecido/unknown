package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;

public class EventRenderHandBase
extends Event {
    private static String primaryObfuscationState;
    private static final EventListeners EVENT_LISTENERS;
    private static String secondaryObfuscationState;

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    static {
        secondaryObfuscationState = null;
        primaryObfuscationState = null;
        EVENT_LISTENERS = new EventListeners();
    }
}
