package vegas.sony.event.impl;

import vegas.sony.event.impl.EventRenderTickBase;
import vegas.sony.utils.render.shader.ShaderProgram;
import vegas.sony.wrapper.impl.DeltaTracker;

public class EventPreRenderTick
extends EventRenderTickBase {
    @Override
    public boolean fire() {
        ShaderProgram.setCurrentProgramId(-1);
        return super.fire();
    }

    public EventPreRenderTick(Object deltaTrackerHandle) {
        super(new DeltaTracker(deltaTrackerHandle));
    }

    public EventPreRenderTick(float f) {
        super(f);
    }
}
