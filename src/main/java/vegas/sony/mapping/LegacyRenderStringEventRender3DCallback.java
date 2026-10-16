package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.EventRender3DCallback;
import vegas.sony.mapping.InsertedCallbackMarker;
import vegas.sony.render.OffscreenRenderContext;
import vegas.sony.wrapper.impl.Minecraft;

public class LegacyRenderStringEventRender3DCallback
extends InsertedCallbackMarker {
    private static final String b = "hand";

    private static Exception a(Exception exception) {
        return exception;
    }

    public static void call(String string) {
        if (string.equals(b) && !OffscreenRenderContext.isRenderingOffscreen()) {
            float f = Minecraft.getTimer().renderPartialTicks();
            EventRender3DCallback eventRender3DCallback = new EventRender3DCallback(f);
            try {
                eventRender3DCallback.fire();
            }
            catch (Exception exception) {
                SonyVegas.logThrowable(exception);
            }
        }
    }
}
