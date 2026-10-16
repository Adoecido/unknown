package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;

public class EventRenderFirstPersonItemBase
extends Event {
    public final float swingProgress;
    public final float equipProgress;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public EventRenderFirstPersonItemBase(float equipProgress, float swingProgress) {
        this.equipProgress = equipProgress;
        this.swingProgress = swingProgress;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }
}
