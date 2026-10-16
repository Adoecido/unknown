package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Entity;

public class EventSetArmorModel
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final float partialTick;
    private int result;
    private final Entity entity;

    public float getPartialTick() {
        return this.partialTick;
    }

    public int getResult() {
        return this.result;
    }

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    public EventSetArmorModel(Object entityHandle, int result, float partialTick) {
        this.entity = new Entity(entityHandle);
        this.result = result;
        this.partialTick = partialTick;
    }
}
