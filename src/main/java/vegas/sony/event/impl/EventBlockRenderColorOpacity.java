package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.WorldRenderer;
import java.nio.ByteOrder;

public class EventBlockRenderColorOpacity
extends Event {
    private int opacity;
    private static final long DEFAULT_OPACITY_BITS;
    private final WorldRenderer worldRenderer;
    private final int vertexIndex;
    private static final EventListeners EVENT_LISTENERS;
    private final float redMultiplier;
    private final float greenMultiplier;
    private final float blueMultiplier;

    static {
        DEFAULT_OPACITY_BITS = 22123904822673663L;
        EVENT_LISTENERS = new EventListeners();
    }


    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return this.isCanceled();
    }

    public EventBlockRenderColorOpacity(Object worldRendererHandle, float redMultiplier, float greenMultiplier, float blueMultiplier, int vertexIndex) {
        this.worldRenderer = new WorldRenderer(worldRendererHandle);
        this.redMultiplier = redMultiplier;
        this.greenMultiplier = greenMultiplier;
        this.blueMultiplier = blueMultiplier;
        this.vertexIndex = vertexIndex;
        this.opacity = (int)DEFAULT_OPACITY_BITS;
    }
}

