package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;

public class EventBlockFluidRender
extends Event {
    private final int z;
    private final int x;
    private final Object blockHandle;
    private final int y;
    private final Object renderBlocksHandle;
    private boolean result;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public boolean isResult() {
        return this.result;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public EventBlockFluidRender(Object renderBlocksHandle, Object blockHandle, int x, int y, int z) {
        this.renderBlocksHandle = renderBlocksHandle;
        this.blockHandle = blockHandle;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean fire() {
        return false;
    }


    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }
}

