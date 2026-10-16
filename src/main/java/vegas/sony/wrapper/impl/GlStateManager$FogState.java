package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class GlStateManager$FogState
extends Wrapper {
    public GlStateManager$FogState(Object fogStateHandle) {
        super(fogStateHandle);
    }

    public void reset() {
        GlStateManager$FogState.sonyVegasInstance.getMappingsMapperCompat().glFogStateObject.reset(this.I);
    }

    public GlStateManagerFogStateBridge getFogMode() {
        Object fogModeHandle = GlStateManager$FogState.sonyVegasInstance.getMappingsMapperCompat().glFogStateObject
                .getCurrent(this.I);
        return new GlStateManagerFogStateBridge(fogModeHandle);
    }
}
