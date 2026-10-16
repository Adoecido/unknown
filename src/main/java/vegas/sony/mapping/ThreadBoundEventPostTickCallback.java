package vegas.sony.mapping;

import vegas.sony.event.impl.EventThreadBoundPostTick;
import vegas.sony.event.impl.EventTickBase;
import vegas.sony.mapping.ThreadBoundTickCallbackBase;

public class ThreadBoundEventPostTickCallback
extends ThreadBoundTickCallbackBase {
    public static void call() {
        if (Thread.currentThread().equals(EventTickBase.PRE_TICK_EXECUTOR.getOwnerThread())) {
            new EventThreadBoundPostTick().fire();
        }
    }

}

