package vegas.sony.event.impl;

import vegas.sony.ui.click.GuiScreenNativeCallbackBridge;
import vegas.sony.utils.RenderThreadTaskQueue;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.RenderBatchManager;

public class EventPostRenderTick
extends EventRenderTickBase {
    private static final String RENDER_PHASE_NAME;
    private static boolean fireLogged;

    public EventPostRenderTick() {
        super(-1.0f);
    }

    public EventPostRenderTick(float f) {
        super(f);
    }


    @Override
    public boolean fire() {
        if (!fireLogged) {
            fireLogged = true;
            vegas.sony.runtime.CheatBreakerDebugLog.info("EventPostRenderTick.fire invoked");
        }
        GuiScreenNativeCallbackBridge.drawScreen(null, 0, 0, 0.0f);
        if (GuiRenderPrimitives.d()) {
            RenderThreadTaskQueue.runPendingTasks();
            RenderBatchManager.getInstance().flushGuiBatches(this.getTicks());
        }
        boolean fired = super.fire();
        GuiRenderPrimitives.l(RENDER_PHASE_NAME);
        return fired;
    }

    static {
        try {
            RENDER_PHASE_NAME = "Post render";
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}

