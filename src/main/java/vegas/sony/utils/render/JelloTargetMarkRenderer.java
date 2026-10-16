package vegas.sony.utils.render;

import vegas.sony.wrapper.impl.GlStateManager;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RenderManager;
import java.awt.Color;
import org.lwjgl.opengl.GL11;

/**
 * Animated target marker based on FDPClient's Jello motion profile.
 */
public final class JelloTargetMarkRenderer {
    private static final int LAYER_COUNT = 20;
    private static final int SEGMENT_COUNT = 72;
    private static final float MIN_RADIUS = 0.10f;
    private static final float BASE_MIN_TRAIL_HEIGHT = 0.064f;
    private static final float BASE_LINE_WIDTH = 3.2f;

    private JelloTargetMarkRenderer() {
    }

    public static void draw(double worldX, double worldY, double worldZ,
                            float entityWidth, float entityHeight, Color color,
                            float widthScale, float thicknessScale, float heightScale,
                            float endpointOffset, long cycleMillis, float alphaScale) {
        if (color == null || entityHeight <= 0.0f) {
            return;
        }

        float safeThickness = Math.max(0.05f, thicknessScale);
        float safeAlphaScale = Math.max(0.0f, Math.min(1.0f, alphaScale));
        AnimationFrame frame = calculateFrame(worldY, entityHeight, heightScale,
                endpointOffset, safeThickness, cycleMillis);
        float radius = Math.max(MIN_RADIUS, entityWidth * Math.max(0.05f, widthScale));
        if (GuiRenderPrimitives.d()) {
            drawBuffered(worldX, worldZ, radius, frame, color,
                    BASE_MIN_TRAIL_HEIGHT * safeThickness, safeAlphaScale);
        } else {
            drawLegacy(worldX, worldZ, radius, frame, color,
                    BASE_LINE_WIDTH * safeThickness, safeAlphaScale);
        }
    }

    private static AnimationFrame calculateFrame(double worldY, float entityHeight,
                                                 float heightScale, float endpointOffset,
                                                 float thicknessScale, long cycleMillis) {
        long safeCycleMillis = Math.max(2L, cycleMillis);
        long drawTime = Math.floorMod(System.currentTimeMillis(), safeCycleMillis);
        boolean descending = drawTime > safeCycleMillis / 2L;
        double progress = drawTime / (safeCycleMillis / 2.0);
        progress = descending ? progress - 1.0 : 1.0 - progress;
        progress = easeInOutQuad(progress);

        double travelHeight = entityHeight * Math.max(0.05f, heightScale);
        double centerY = worldY + (descending ? -endpointOffset : endpointOffset)
                + travelHeight * progress;
        double baseMove = progress > 0.5 ? 1.0 - progress : progress;
        double alignment = travelHeight / 60.0 * LAYER_COUNT * (1.0 - baseMove)
                * (descending ? -1.0 : 1.0);
        double layerStep = travelHeight / 60.0 * baseMove * thicknessScale;
        return new AnimationFrame(centerY, alignment, layerStep, descending);
    }

    private static void drawBuffered(double worldX, double worldZ, float radius,
                                     AnimationFrame frame, Color color,
                                     float minimumTrailHeight, float alphaScale) {
        float centerX = (float)(worldX - RenderManager.getInterpolatedRenderPosX());
        float centerZ = (float)(worldZ - RenderManager.getInterpolatedRenderPosZ());
        float cameraY = (float)RenderManager.getInterpolatedRenderPosY();
        float transparentY = (float)(frame.getLayerY(0) - cameraY);
        float visibleY = (float)(frame.getLayerY(LAYER_COUNT) - cameraY);
        if (Math.abs(visibleY - transparentY) < minimumTrailHeight) {
            transparentY = visibleY + (frame.descending ? -minimumTrailHeight : minimumTrailHeight);
        }
        Color transparentColor = withLayerAlpha(color, 0, alphaScale);
        Color visibleColor = withLayerAlpha(color, LAYER_COUNT, alphaScale);
        boolean blendEnabled = OpenGlBackendHolder.backend.isCapabilityEnabled(GL11.GL_BLEND);
        boolean depthEnabled = OpenGlBackendHolder.backend.isCapabilityEnabled(GL11.GL_DEPTH_TEST);
        boolean cullEnabled = OpenGlBackendHolder.backend.isCapabilityEnabled(GL11.GL_CULL_FACE);
        boolean depthWriteEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderUtil.d();
        Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf().B(1.0);
        try {
            OpenGlBackendHolder.backend.enableCapability(GL11.GL_BLEND);
            OpenGlBackendHolder.backend.disableCapability(GL11.GL_DEPTH_TEST);
            OpenGlBackendHolder.backend.disableCapability(GL11.GL_CULL_FACE);
            OpenGlBackendHolder.backend.setDepthMask(false);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            float previousX = circleX(centerX, radius, 0);
            float previousZ = circleZ(centerZ, radius, 0);
            for (int segment = 1; segment <= SEGMENT_COUNT; ++segment) {
                float currentX = circleX(centerX, radius, segment);
                float currentZ = circleZ(centerZ, radius, segment);
                BufferedRenderPrimitives.fillGradientQuad(
                        currentX, transparentY, currentZ,
                        previousX, transparentY, previousZ,
                        previousX, visibleY, previousZ,
                        currentX, visibleY, currentZ,
                        transparentColor, visibleColor);
                previousX = currentX;
                previousZ = currentZ;
            }
        } finally {
            OpenGlBackendHolder.backend.setDepthMask(depthWriteEnabled);
            restoreCapability(GL11.GL_CULL_FACE, cullEnabled);
            restoreCapability(GL11.GL_DEPTH_TEST, depthEnabled);
            restoreCapability(GL11.GL_BLEND, blendEnabled);
            Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf().O(1.0);
            RenderUtil.Y();
        }
    }

    private static void drawLegacy(double worldX, double worldZ, float radius,
                                   AnimationFrame frame, Color color,
                                   float lineWidth, float alphaScale) {
        double centerX = worldX - RenderManager.getInterpolatedRenderPosX();
        double centerZ = worldZ - RenderManager.getInterpolatedRenderPosZ();
        double cameraY = RenderManager.getInterpolatedRenderPosY();
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean lineSmoothEnabled = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
        boolean depthWriteEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        float previousLineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);

        RenderUtil.d();
        Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf().B(1.0);
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableDepth();
            GlStateManager.disableTexture2D();
            OpenGlBackendHolder.backend.enableCapability(GL11.GL_LINE_SMOOTH);
            OpenGlBackendHolder.backend.setDepthMask(false);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            OpenGlBackendHolder.backend.setLineWidth(lineWidth);

            for (int layer = 0; layer <= LAYER_COUNT; ++layer) {
                Color layerColor = withLayerAlpha(color, layer, alphaScale);
                RenderUtils.w(layerColor);
                double layerY = frame.getLayerY(layer) - cameraY;
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int segment = 0; segment <= SEGMENT_COUNT; ++segment) {
                    GL11.glVertex3d(circleX(centerX, radius, segment), layerY,
                            circleZ(centerZ, radius, segment));
                }
                GL11.glEnd();
            }
        } finally {
            OpenGlBackendHolder.backend.setLineWidth(previousLineWidth);
            OpenGlBackendHolder.backend.setDepthMask(depthWriteEnabled);
            restoreCapability(GL11.GL_LINE_SMOOTH, lineSmoothEnabled);
            restoreCapability(GL11.GL_TEXTURE_2D, textureEnabled);
            restoreCapability(GL11.GL_DEPTH_TEST, depthEnabled);
            restoreCapability(GL11.GL_BLEND, blendEnabled);
            RenderUtils.w(Color.WHITE);
            Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf().O(1.0);
            RenderUtil.Y();
        }
    }

    private static Color withLayerAlpha(Color color, int layer, float alphaScale) {
        float fade = layer / (float)LAYER_COUNT;
        int sourceAlpha = Math.max(80, color.getAlpha());
        int alpha = Math.min(255, Math.round(sourceAlpha * alphaScale * fade));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static float circleX(float centerX, float radius, int segment) {
        double angle = Math.PI * 2.0 * segment / SEGMENT_COUNT;
        return centerX - (float)Math.sin(angle) * radius;
    }

    private static float circleZ(float centerZ, float radius, int segment) {
        double angle = Math.PI * 2.0 * segment / SEGMENT_COUNT;
        return centerZ + (float)Math.cos(angle) * radius;
    }

    private static double circleX(double centerX, float radius, int segment) {
        double angle = Math.PI * 2.0 * segment / SEGMENT_COUNT;
        return centerX - Math.sin(angle) * radius;
    }

    private static double circleZ(double centerZ, float radius, int segment) {
        double angle = Math.PI * 2.0 * segment / SEGMENT_COUNT;
        return centerZ + Math.cos(angle) * radius;
    }

    private static void restoreCapability(int capability, boolean enabled) {
        if (enabled) {
            OpenGlBackendHolder.backend.enableCapability(capability);
        } else {
            OpenGlBackendHolder.backend.disableCapability(capability);
        }
    }

    private static double easeInOutQuad(double value) {
        return value < 0.5
                ? 2.0 * value * value
                : 1.0 - Math.pow(-2.0 * value + 2.0, 2.0) / 2.0;
    }

    private static final class AnimationFrame {
        private final double centerY;
        private final double alignment;
        private final double layerStep;
        private final boolean descending;

        private AnimationFrame(double centerY, double alignment, double layerStep,
                               boolean descending) {
            this.centerY = centerY;
            this.alignment = alignment;
            this.layerStep = layerStep;
            this.descending = descending;
        }

        private double getLayerY(int layer) {
            double layerOffset = this.layerStep * layer;
            if (this.descending) {
                layerOffset = -layerOffset;
            }
            return this.centerY - layerOffset - this.alignment;
        }
    }
}
