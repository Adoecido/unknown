package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.EntityPlayer;

public class EventPreRenderPlayerSpec
extends Event {
    private final float partialTicks;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final EntityPlayer player;

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    public float getPartial() {
        return this.partialTicks;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public EntityPlayer getClientPlayer() {
        return this.player;
    }

    public EventPreRenderPlayerSpec(Object playerHandle, float partialTicks) {
        this.player = new EntityPlayer(playerHandle);
        this.partialTicks = partialTicks;
    }
}
