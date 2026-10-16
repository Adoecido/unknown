package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Entity;

public class EventSetSprinting
extends Event {
    private final boolean newSprintingState;
    private final Entity entity;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public EventSetSprinting(Object entityHandle, boolean newSprintingState) {
        this.entity = new Entity(entityHandle);
        this.newSprintingState = newSprintingState;
    }

    public boolean isNewStateSprinting() {
        return this.newSprintingState;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }
}
