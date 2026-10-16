package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import vegas.sony.utils.render.GlImageTexture;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.utils.render.RenderBatchBuilder;
import vegas.sony.utils.render.RenderBatchManager;
import vegas.sony.wrapper.impl.GlStateManager;
import vegas.sony.wrapper.impl.Minecraft;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import org.lwjgl.opengl.GL11;

public class BlurRegionRenderer {
    private int textureId = -1;
    private int regionWidth;
    private boolean textureInitialized = false;
    private static boolean legacyGlobalFlag;
    private int regionHeight;

    public BlurRegionRenderer(int width, int height) {
        this.regionWidth = width;
        this.regionHeight = height;
    }

    private Void copyHorizontalPassSource(int x, float captureScale, int sourceY) {
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
        return null;
    }

    public void setDimensions(int width, int height) {
        if (this.regionWidth == width && this.regionHeight == height) {
            return;
        }
        this.regionWidth = width;
        this.regionHeight = height;
        this.textureInitialized = false;
    }

    public static boolean getLegacyGlobalFlag() {
        return legacyGlobalFlag;
    }


    public static void setLegacyGlobalFlag(boolean enabled) {
        legacyGlobalFlag = enabled;
    }

    public void renderBlur(int x, int y, float blurRadius, float inset) {
        RenderBatchBuilder blurBatch;
        Supplier<Void> drawSetupCallback;
        if (!Minecraft.gameSettings().Y$src$Z$1rxemad()) {
            return;
        }
        float captureScale = 2.0f;
        double uiScale = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        x = (int)((double)x * uiScale);
        y = (int)((double)y * uiScale);
        int framebufferHeight = Minecraft.h();
        int sourceY = (int)((float)framebufferHeight - (float)y * captureScale - (float)this.regionHeight);
        if (!this.textureInitialized) {
            this.initializeTexture();
        }
        float renderScale = 0.5f;
        renderScale = (float)((double)renderScale / uiScale);
        OpenGlBackendHolder.backend.pushMatrix();
        if (!GuiRenderPrimitives.d()) {
            OpenGlBackendHolder.backend.scale(renderScale, renderScale, renderScale);
        }
        if (!GuiRenderPrimitives.d()) {
            GL11.glBindTexture((int)3553, (int)this.textureId);
        }
        if (GuiRenderPrimitives.d()) {
            final int capturedX = x;
            drawSetupCallback = () -> this.copyHorizontalPassSource(capturedX, captureScale, sourceY);
            if (this.regionWidth == 0 || this.regionHeight == 0) {
                return;
            }
            blurBatch = new RenderBatchBuilder().setDrawSetupCallback(drawSetupCallback).setTexture(new GlImageTexture(this.textureId)).addBlurPass(x, y, (float)this.regionWidth / 2.0f, (float)this.regionHeight / 2.0f, blurRadius, inset, 1.0f, 0.0f);
            RenderBatchManager.getInstance().queueGuiBatch(blurBatch);
        } else {
            GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
            GuiRenderPrimitives.v((float)x * captureScale, (float)y * captureScale, this.regionWidth, this.regionHeight, blurRadius, inset, 1.0f, 0);
        }
        if (GuiRenderPrimitives.d()) {
            final int capturedX = x;
            drawSetupCallback = () -> this.copyVerticalPassSource(capturedX, captureScale, sourceY);
            if (this.regionWidth == 0 || this.regionHeight == 0) {
                return;
            }
            blurBatch = new RenderBatchBuilder().setDrawSetupCallback(drawSetupCallback).setTexture(new GlImageTexture(this.textureId)).addBlurPass(x, y, (float)this.regionWidth / 2.0f, (float)this.regionHeight / 2.0f, blurRadius, inset, 1.0f, 1.0f);
            RenderBatchManager.getInstance().queueGuiBatch(blurBatch);
        } else {
            GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
            GuiRenderPrimitives.v((float)x * captureScale, (float)y * captureScale, this.regionWidth, this.regionHeight, blurRadius, inset, 1.0f, 1);
        }
        OpenGlBackendHolder.backend.popMatrix();
        GlStateManager.bindTexture(0);
    }

    /**
     * Blurs this region once and paints it back only inside the given pieces,
     * each rounded on its leading edge. One capture for the whole shape means
     * the pieces share a continuous blur, instead of each one blurring against
     * its own edges and reading as a separate box.
     *
     * The pieces are x, y, width, height in the caller's own coordinates, the
     * same ones renderBlur takes, and must lie inside the region that starts at
     * (x, y) and is regionWidth by regionHeight framebuffer pixels.
     */
    public void renderBlurPieces(int x, int y, float blurRadius, float cornerRadius,
                                 double[] pieces) {
        if (!Minecraft.gameSettings().Y$src$Z$1rxemad()) {
            return;
        }
        if (pieces == null || pieces.length < 4) {
            return;
        }
        if (this.regionWidth == 0 || this.regionHeight == 0) {
            return;
        }
        float captureScale = 2.0f;
        double uiScale = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        double originX = (double)x * uiScale;
        double originY = (double)y * uiScale;
        int framebufferHeight = Minecraft.h();
        int sourceY = (int)((float)framebufferHeight - (float)originY * captureScale - (float)this.regionHeight);
        if (!this.textureInitialized) {
            this.initializeTexture();
        }
        if (GuiRenderPrimitives.d()) {
            // The modern backend batches its blur passes and has no sub-rect
            // draw, so each piece keeps its own capture there.
            for (int i = 0; i + 3 < pieces.length; i += 4) {
                this.setDimensions((int)Math.round(pieces[i + 2] * uiScale * captureScale),
                        (int)Math.round(pieces[i + 3] * uiScale * captureScale));
                this.renderBlur((int)Math.round(pieces[i]), (int)Math.round(pieces[i + 1]),
                        blurRadius, cornerRadius);
            }
            return;
        }
        float renderScale = 0.5f / (float)uiScale;
        OpenGlBackendHolder.backend.pushMatrix();
        OpenGlBackendHolder.backend.scale(renderScale, renderScale, renderScale);
        GL11.glBindTexture((int)3553, (int)this.textureId);
        for (int pass = 0; pass < 2; ++pass) {
            GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0,
                    (int)((float)originX * captureScale), sourceY, this.regionWidth, this.regionHeight);
            for (int i = 0; i + 3 < pieces.length; i += 4) {
                double pieceX = (pieces[i] * uiScale - originX) * (double)captureScale;
                double pieceY = (pieces[i + 1] * uiScale - originY) * (double)captureScale;
                double pieceWidth = pieces[i + 2] * uiScale * (double)captureScale;
                double pieceHeight = pieces[i + 3] * uiScale * (double)captureScale;
                GuiRenderPrimitives.blurPiece(
                        originX * (double)captureScale + pieceX,
                        originY * (double)captureScale + pieceY,
                        pieceWidth, pieceHeight,
                        (float)this.regionWidth, (float)this.regionHeight,
                        (float)pieceX, (float)pieceY, (float)pieceWidth, (float)pieceHeight,
                        blurRadius, cornerRadius, pass);
            }
        }
        OpenGlBackendHolder.backend.popMatrix();
        GlStateManager.bindTexture(0);
    }

    public static boolean isBlurAvailable() {
        boolean ignoredLegacyFlag = BlurRegionRenderer.getLegacyGlobalFlag();
        return true;
    }

    private Void copyVerticalPassSource(int x, float captureScale, int sourceY) {
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
        return null;
    }

    private void initializeTexture() {
        // Every size change allocates a new texture. Callers that resize their
        // region often (a per-row HUD list) would leak one texture per change,
        // so the previous one is released first.
        if (this.textureId != -1) {
            GL11.glDeleteTextures((int)this.textureId);
        }
        this.textureId = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)6407, (int)this.regionWidth, (int)this.regionHeight, (int)0, (int)6407, (int)5121, (ByteBuffer)null);
        this.textureInitialized = true;
    }

    static {
        BlurRegionRenderer.setLegacyGlobalFlag(false);
    }
}
