package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Block;

public class EventBlockShouldRender
extends Event {
    private final Block block;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public EventBlockShouldRender(Object blockHandle) {
        this.block = new Block(blockHandle);
    }

    @Override
    public boolean fire() {
        return false;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public Block getBlock() {
        return this.block;
    }


    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }
}

