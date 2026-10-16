package vegas.sony.mapping;

import vegas.sony.event.impl.EventPostTick;
import vegas.sony.mapping.InsertedTickCallbackBase;

public class EventPostTickCallback
extends InsertedTickCallbackBase {
    public static void call() {
        new EventPostTick().fire();
    }
}

