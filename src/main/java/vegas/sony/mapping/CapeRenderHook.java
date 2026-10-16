package vegas.sony.mapping;

import org.lwjgl.opengl.GL11;

/** Badlion cape callbacks and the explicit renderer for bundled cape atlases. */
public final class CapeRenderHook {
    private static final ThreadLocal<Boolean> CUSTOM_CAPE = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private CapeRenderHook() {
    }

    public static void renderPlayerEnter(Object playerHandle) {
    }

    public static void renderPlayerExit(Object playerHandle) {
    }

    public static void capeLayerEnter(Object playerHandle) {
        CUSTOM_CAPE.set(Boolean.FALSE);
    }

    public static void capeLayerExit(Object playerHandle) {
        CUSTOM_CAPE.remove();
    }

    /** Called by the getLocationCape hook while the current LayerCape invocation is active. */
    public static void markCustomCape(boolean active) {
        CUSTOM_CAPE.set(Boolean.valueOf(active));
    }

    /**
     * Replaces Badlion's final ModelPlayer#renderCape call for our texture only.
     * LayerCape has already bound the texture and applied its movement transforms
     * at this point. Drawing with normalized atlas coordinates avoids Badlion's
     * modified ModelRenderer selecting a black or cropped region of the HD PNG.
     */
    public static boolean renderCustomCape(float scale) {
        if (!Boolean.TRUE.equals(CUSTOM_CAPE.get())) return false;

        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            if (cullEnabled) GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            float left = -5.0F * scale;
            float right = 5.0F * scale;
            float top = 0.0F;
            float bottom = 16.0F * scale;
            float outerZ = -1.0F * scale;
            float innerZ = 0.0F;

            // The source is a 16x vanilla cape atlas. These normalized UVs
            // select (1,1)-(11,17) and (12,1)-(22,17) at every resolution.
            float firstU0 = 1.0F / 64.0F;
            float firstU1 = 11.0F / 64.0F;
            float secondU0 = 12.0F / 64.0F;
            float secondU1 = 22.0F / 64.0F;
            float v0 = 1.0F / 32.0F;
            float v1 = 17.0F / 32.0F;

            GL11.glBegin(GL11.GL_QUADS);

            // Outer face, visible from the rear third-person camera.
            GL11.glNormal3f(0.0F, 0.0F, -1.0F);
            vertex(secondU0, v0, left, top, outerZ);
            vertex(secondU1, v0, right, top, outerZ);
            vertex(secondU1, v1, right, bottom, outerZ);
            vertex(secondU0, v1, left, bottom, outerZ);

            // Inner face. The source contains a separate matching panel.
            GL11.glNormal3f(0.0F, 0.0F, 1.0F);
            vertex(firstU1, v0, right, top, innerZ);
            vertex(firstU0, v0, left, top, innerZ);
            vertex(firstU0, v1, left, bottom, innerZ);
            vertex(firstU1, v1, right, bottom, innerZ);

            GL11.glEnd();
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            if (cullEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    private static void vertex(float u, float v, float x, float y, float z) {
        GL11.glTexCoord2f(u, v);
        GL11.glVertex3f(x, y, z);
    }
}
