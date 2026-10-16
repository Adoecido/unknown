package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.Tessellator;

public class EventLegacyXRayBlockRenderBase
extends Event {
    private static boolean obfuscationState;
    private final Tessellator tessellator;
    private static final EventListeners EVENT_LISTENERS;
    private final Block block;

    public Tessellator getTessellator() {
        return this.tessellator;
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

    public static void setXRayObfuscationState(boolean state) {
        obfuscationState = state;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }


    public EventLegacyXRayBlockRenderBase(Object blockHandle) {
        this.block = new Block(blockHandle);
        this.tessellator = Tessellator.getInstance();
    }

    public static boolean getXRayObfuscationState() {
        return obfuscationState;
    }

    public static boolean getObfuscationConstant() {
        boolean state = EventLegacyXRayBlockRenderBase.getXRayObfuscationState();
        return true;
    }

    static {
        EVENT_LISTENERS = new EventListeners();
        EventLegacyXRayBlockRenderBase.setXRayObfuscationState(false);
    }
}

