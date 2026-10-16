package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import vegas.sony.utils.render.GlImageTexture;
import vegas.sony.utils.render.ImageRenderer;
import java.awt.Color;

public class LocalImageDebugRenderer {
    private static String legacyStatus;
    static final String DEBUG_IMAGE_PATH;

    public static String getLegacyStatus() {
        return legacyStatus;
    }

    private static Exception propagateException(Exception exception) {
        return exception;
    }

    public static GlImageTexture loadDebugTexture() {
        // Release build: dev-only texture loading is disabled, so the client
        // never touches the filesystem here.
        return null;
    }

    public static void renderDebugImage() {
        try {
            GlImageTexture texture = LocalImageDebugRenderer.loadDebugTexture();
            if (texture != null) {
                SonyVegas.debugLog("drawing");
                ImageRenderer.drawTexture(Color.BLACK, 10.0f, 10.0f, texture, 20.0f, 20.0f, true);
            }
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    static {
        LocalImageDebugRenderer.setLegacyStatus(null);
        DEBUG_IMAGE_PATH = "";
    }

    public static void setLegacyStatus(String status) {
        legacyStatus = status;
    }
}
