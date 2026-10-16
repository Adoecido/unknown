package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import java.util.ArrayList;
import java.util.Collection;

public class EventScoreboardScores
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private static boolean locked;

    @Override
    public boolean fire() {
        return locked;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public Collection getScores() {
        return new ArrayList();
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static void setLocked(boolean locked) {
        EventScoreboardScores.locked = locked;
    }
}
