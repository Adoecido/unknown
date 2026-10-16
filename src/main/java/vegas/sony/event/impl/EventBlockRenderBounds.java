package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.RenderBlocks;

public class EventBlockRenderBounds
extends Event {
    private final RenderBlocks renderBlocks;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final Block block;

    public RenderBlocks getRenderBlocks() {
        return this.renderBlocks;
    }

    public EventBlockRenderBounds(Object renderBlocksHandle, Object blockHandle) {
        this.renderBlocks = new RenderBlocks(renderBlocksHandle);
        this.block = new Block(blockHandle);
    }

    public Block getBlock() {
        return this.block;
    }

    @Override
    public boolean fire() {
        return false;
    }


    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }
}

