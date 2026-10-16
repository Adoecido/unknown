package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.EnumWorldBlockLayer;

public class EventBlockLayerOverride
extends Event {
    private boolean shouldRender;
    private final Block block;
    private static int obfuscationState;
    private static final EventListeners EVENT_LISTENERS;

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }


    public Block getBlock() {
        return this.block;
    }

    @Override
    public boolean fire() {
        return false;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public EventBlockLayerOverride(Object blockHandle) {
        this.block = new Block(blockHandle);
    }

    public void setShouldRender(boolean shouldRender) {
        this.shouldRender = shouldRender;
    }

    public static int getBlockLayerObfuscationState() {
        return obfuscationState;
    }

    public static void setBlockLayerObfuscationState(int state) {
        obfuscationState = state;
    }

    public static int getObfuscationConstant() {
        int state = EventBlockLayerOverride.getBlockLayerObfuscationState();
        return 2;
    }

    public Object getBlockLayer() {
        return this.shouldRender ? EnumWorldBlockLayer.solid().getObject() : EnumWorldBlockLayer.translucent().getObject();
    }

    static {
        EVENT_LISTENERS = new EventListeners();
        EventBlockLayerOverride.setBlockLayerObfuscationState(0);
    }
}

