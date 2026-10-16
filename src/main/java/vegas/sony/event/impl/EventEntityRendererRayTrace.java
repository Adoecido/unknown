package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.mapping.MappedClasses;

public class EventEntityRendererRayTrace
extends Event {
    private static final EventListeners EVENT_LISTENERS;
    private final float partialTicks;
    private final Object entityHandle;

    public EventEntityRendererRayTrace(Object entityHandle, float partialTicks) {
        this.entityHandle = entityHandle;
        this.partialTicks = partialTicks;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return false;
    }

    public Object getVec() {
        return SonyVegas.INSTANCE.getMappingsMapperCompat().Rr.jL.invokeNativeBridge(this.entityHandle, Float.valueOf(this.partialTicks));
    }


    static {
        EVENT_LISTENERS = new EventListeners();
    }
}

