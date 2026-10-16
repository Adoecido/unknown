package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRenderTypeBufferBridge;
import vegas.sony.wrapper.Wrapper;

public class RenderTypeBuffer
extends Wrapper {
    public RenderTypeBuffer(Object handle) {
        super(handle);
    }

    public void onInputReceived() {
        MRenderTypeBufferBridge.onInputReceived(RenderTypeBuffer.sonyVegasInstance.getMappingsMapperCompat().framerateLimitTracker, this.getObject());
    }
}
