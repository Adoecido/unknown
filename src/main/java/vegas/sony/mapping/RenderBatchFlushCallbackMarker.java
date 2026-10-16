package vegas.sony.mapping;

import vegas.sony.mapping.InsertedCallbackMarker;
import vegas.sony.utils.render.RenderBatchManager;

public class RenderBatchFlushCallbackMarker
extends InsertedCallbackMarker {
    public static void call() {
        RenderBatchManager.getInstance().refreshTargetFramebuffer();
    }
}

