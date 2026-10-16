package vegas.sony.mapping;

import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.InsertedTickCallbackBase;

public class EventPreTickCallback
extends InsertedTickCallbackBase {
    public static void call() {
        new EventPreTick().fire();
    }
}

