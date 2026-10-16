package vegas.sony.event.impl;

import vegas.sony.event.impl.EventTickBase;
import vegas.sony.input.InputEventDispatcher;

public class EventPreTick
extends EventTickBase {
    @Override
    public boolean fire() {
        PRE_TICK_EXECUTOR.runPending();
        InputEventDispatcher.getInstance().pollBoundInputs();
        return super.fire();
    }
}
