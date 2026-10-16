package vegas.sony.utils.render;

import vegas.sony.utils.render.BufferedOpenGlBackend;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.LegacyOpenGlBackend;
import vegas.sony.utils.render.OpenGlBackend;

public class OpenGlBackendHolder {
    public static OpenGlBackend backend = GuiRenderPrimitives.d() ? new BufferedOpenGlBackend() : new LegacyOpenGlBackend();

}

