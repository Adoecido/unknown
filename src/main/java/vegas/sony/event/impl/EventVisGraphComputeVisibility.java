package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.SetVisibility;

public class EventVisGraphComputeVisibility
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return false;
    }


    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public EventVisGraphComputeVisibility() {
    }

    public EventVisGraphComputeVisibility(Object ignoredVisGraphHandle) {
    }

    public static Object getVisibility() {
        SetVisibility setVisibility = new SetVisibility(SonyVegas.INSTANCE.getMappingsMapperCompat().setVisibility.constructor.newInstance(new Object[0]));
        setVisibility.setAllVisible(true);
        return setVisibility.getObject();
    }
}

