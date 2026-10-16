package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.RenderBatchManager;
import vegas.sony.wrapper.impl.MatrixStack;

public abstract class EventRender3DBase
extends Event {
    private final float partialTicks;
    private final MatrixStack matrixStack;
    private static GuiComponent[] obfuscationState;

    EventRender3DBase(MatrixStack matrixStack, float partialTicks) {
        this.matrixStack = matrixStack;
        this.partialTicks = partialTicks;
    }

    public static GuiComponent[] getRender3DObfuscationState() {
        return obfuscationState;
    }

    public float getTicks() {
        return this.partialTicks;
    }

    public MatrixStack getMatrixStack() {
        return this.matrixStack;
    }

    @Override
    public boolean fire() {
        boolean fired = super.fire();
        if (GuiRenderPrimitives.d()) {
            RenderBatchManager.getInstance().flushWorldBatches(this.partialTicks);
        }
        return fired;
    }


    public static void setRender3DObfuscationState(GuiComponent[] state) {
        obfuscationState = state;
    }

    static {
        if (EventRender3DBase.getRender3DObfuscationState() == null) {
            EventRender3DBase.setRender3DObfuscationState(new GuiComponent[5]);
        }
    }
}

