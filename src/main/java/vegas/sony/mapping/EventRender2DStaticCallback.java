package vegas.sony.mapping;

import vegas.sony.event.impl.EventRender2D;
import vegas.sony.mapping.InsertedCallbackMarker;

public class EventRender2DStaticCallback
extends InsertedCallbackMarker {
    public static void call() {
        EventRender2D.create();
    }
}

