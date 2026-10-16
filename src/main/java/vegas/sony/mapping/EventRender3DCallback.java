package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.event.impl.EventRenderTracers3D;
import vegas.sony.mapping.InsertedEventCallback;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.wrapper.impl.EntityRenderer;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GameSettings;
import vegas.sony.wrapper.impl.GlStateManager;
import vegas.sony.wrapper.impl.MatrixStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RenderManager;
import org.lwjgl.opengl.GL11;

public class EventRender3DCallback
implements InsertedEventCallback {
    private final float N;
    private static boolean r;
    private static float D;
    private final MatrixStack y;

    private static Exception a(Exception exception) {
        return exception;
    }

    @Override
    public boolean fire() {
        RenderManager.updateInterpolatedRenderPosition(this.N);
        boolean bl = EventRender3D.getEventListeners().hasListeners() || EventRenderTracers3D.getEventListeners().hasListeners();
        boolean bl2 = false;
        boolean bl3 = false;
        if (EventRender3D.getEventListeners().hasListeners()) {
            bl2 = new EventRender3D(this.y, this.N).fire();
        }
        GameSettings gameSettings = Minecraft.gameSettings();
        EntityRenderer entityRenderer = Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf();
        if (EventRenderTracers3D.getEventListeners().hasListeners()) {
            if (ForgeVersion.MC_1_16_5.d()) {
                try {
                    boolean bl4 = gameSettings.k();
                    gameSettings.O(false);
                    entityRenderer.s(this.N, 0);
                    bl3 = new EventRenderTracers3D(this.y, this.N).fire();
                    gameSettings.O(bl4);
                }
                catch (Exception exception) {
                    SonyVegas.logThrowable(exception);
                }
            } else {
                SharedModuleControlClaims.renderPass.blockRender();
                GL11.glPushMatrix();
                GlStateManager.F$src$V$acq27m();
                entityRenderer.Y(this.N);
                bl3 = new EventRenderTracers3D(this.y, this.N).fire();
                GL11.glPopMatrix();
                SharedModuleControlClaims.renderPass.clearClaimed();
            }
        }
        if (bl) {
            entityRenderer.B(1.0);
            OpenGlBackendHolder.backend.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        D = this.N;
        return bl2 || bl3;
    }

    public EventRender3DCallback(Object object) {
        this.y = new MatrixStack(object);
        this.N = Minecraft.getTimer().renderPartialTicks();
    }

    static {
        D = -1.0f;
    }

    public EventRender3DCallback(float f) {
        this.y = null;
        this.N = f;
    }
}

