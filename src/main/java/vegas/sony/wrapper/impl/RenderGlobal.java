package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class RenderGlobal
extends Wrapper {
    public RenderGlobal(Object renderGlobalHandle) {
        super(renderGlobalHandle);
    }

    public void loadRenderers() {
        RenderGlobal.sonyVegasInstance.getMappingsMapperCompat().renderGlobal.loadRenderers(this.I);
    }
}
