package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.Entity;

@Deprecated
public class DeprecatedEntityPlayerUpdateEventBase
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final Entity entity;

    public DeprecatedEntityPlayerUpdateEventBase(Object entityHandle) {
        this.entity = new Entity(entityHandle);
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }


    @Override
    public boolean fire() {
        if (!this.entity.isInstance(MappedClasses.z5)) {
            return false;
        }
        return super.fire();
    }
}

