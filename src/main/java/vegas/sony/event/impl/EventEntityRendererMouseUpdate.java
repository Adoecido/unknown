package vegas.sony.event.impl;

import vegas.sony.event.impl.EventEntityRendererMouseUpdateBase;
import vegas.sony.wrapper.impl.DeltaTracker;

public class EventEntityRendererMouseUpdate
extends EventEntityRendererMouseUpdateBase {
    private final float partialTicks;

    public EventEntityRendererMouseUpdate(Object deltaTrackerHandle) {
        this.partialTicks = new DeltaTracker(deltaTrackerHandle).getGameTimeDeltaPartialTick(true);
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }
}
