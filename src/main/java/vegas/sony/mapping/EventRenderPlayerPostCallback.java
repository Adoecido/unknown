package vegas.sony.mapping;

import vegas.sony.event.impl.EventRenderPlayerPost;
import vegas.sony.mapping.AbstractEventRenderPlayerCallback;

public class EventRenderPlayerPostCallback
extends AbstractEventRenderPlayerCallback {
    public static void call(Object object, Object object2, Object object3) {
        if (!AbstractEventRenderPlayerCallback.access$000(object)) {
            return;
        }
        new EventRenderPlayerPost(object, object2, object3).fire();
    }

}

