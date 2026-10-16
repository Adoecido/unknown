package vegas.sony.event.impl;

import vegas.sony.event.EventListeners;
import vegas.sony.event.impl.EventRender3DBase;
import vegas.sony.wrapper.impl.MatrixStack;

public class EventRenderTracers3D
extends EventRender3DBase {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public EventRenderTracers3D(MatrixStack bm_02, float f) {
        super(bm_02, f);
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }
}
