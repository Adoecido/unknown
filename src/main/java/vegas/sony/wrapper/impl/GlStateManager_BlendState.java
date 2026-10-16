package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class GlStateManager_BlendState
extends Wrapper {
    private GlStateManager$BooleanState blendEnabledState;

    public GlStateManager$BooleanState getBlendEnabledState() {
        if (this.blendEnabledState == null) {
            Object stateHandle = GlStateManager_BlendState.sonyVegasInstance.getMappingsMapperCompat().glBlendState
                    .getBlendEnabledState(this.I);
            this.blendEnabledState = new GlStateManager$BooleanState(stateHandle);
        }
        return this.blendEnabledState;
    }

    public GlStateManager_BlendState(Object blendStateHandle) {
        super(blendStateHandle);
    }
}

