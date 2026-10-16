package vegas.sony.event.impl;

import vegas.sony.event.impl.EventTickBase;

public class EventPostTick
extends EventTickBase {
    @Override
    public boolean fire() {
        POST_TICK_EXECUTOR.runPending();
        return super.fire();
    }
}
