package vegas.sony.mapping;

import vegas.sony.event.impl.EventRenderPlayerPre;
import vegas.sony.mapping.AbstractEventRenderPlayerCallback;

public class EventRenderPlayerPreCallback
extends AbstractEventRenderPlayerCallback {

    public static boolean call(Object object, Object object2, Object object3) {
        if (!AbstractEventRenderPlayerCallback.access$000(object)) {
            return false;
        }
        return new EventRenderPlayerPre(object, object2, object3).fire();
    }
}

