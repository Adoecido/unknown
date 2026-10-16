package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.EnumWorldBlockLayer;

public class EventBlockLayerRender
extends Event {
    private final EnumWorldBlockLayer blockLayer;
    private final Block block;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private boolean shouldRender;

    public void setShouldRender(boolean shouldRender) {
        this.shouldRender = shouldRender;
    }

    @Override
    public boolean fire() {
        return false;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }


    public boolean shouldRender() {
        return this.shouldRender;
    }

    public EventBlockLayerRender(Object blockHandle, Object blockLayerHandle) {
        this.block = new Block(blockHandle);
        this.blockLayer = new EnumWorldBlockLayer(blockLayerHandle);
    }

    public EnumWorldBlockLayer getEnumWorldBlockLayer() {
        return this.blockLayer;
    }

    public Block getBlock() {
        return this.block;
    }
}

