package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.utils.render.BufferedGuiRenderPrimitives;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.RenderBatchBuilder;
import vegas.sony.utils.render.RenderBatchManager;
import vegas.sony.utils.render.RenderMatrix4f;
import vegas.sony.utils.render.RenderVector4f;
import vegas.sony.utils.render.VertexCoordinateMode;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.ForgeVersion;
import java.util.function.Supplier;

public class GuiInventory
extends Wrapper {
    private static Void renderQueuedEntity(RenderMatrix4f renderMatrix, int scale, float mouseX, float mouseY, EntityLivingBase entity) {
        SonyVegas.INSTANCE.getMappings().guiInventory.drawEntityOnScreen(
                (int)renderMatrix.elements[0],
                (int)renderMatrix.elements[5],
                (int)((double)scale * SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor()),
                mouseX,
                mouseY,
                entity.getObject());
        return null;
    }

    public static void drawEntityOnScreen(int screenX, int screenY, int scale, float mouseX, float mouseY, EntityLivingBase entity) {
        if (ForgeVersion.MC_1_20_6.d()) {
            SonyVegas.notifyNativeStackTrace();
        }
        if (GuiRenderPrimitives.d()) {
            RenderMatrix4f renderMatrix = new RenderMatrix4f(new RenderVector4f(screenX, screenY, 0.0f, 1.0f))
                    .multiply(BufferedGuiRenderPrimitives.matrixStack.peek());
            Supplier<Void> renderCallback = () -> GuiInventory.renderQueuedEntity(
                    renderMatrix, scale, mouseX, mouseY, entity);
            RenderBatchBuilder renderBatch = new RenderBatchBuilder(VertexCoordinateMode.MINECRAFT, false)
                    .setStandaloneRenderCallback(renderCallback);
            RenderBatchManager.getInstance().queueGuiBatch(renderBatch);
            return;
        }
        SonyVegas.INSTANCE.getMappings().guiInventory.drawEntityOnScreen(
                screenX, screenY, scale, mouseX, mouseY, entity.getObject());
    }

    public GuiInventory(Object inventoryScreenHandle) {
        super(inventoryScreenHandle);
    }

}

