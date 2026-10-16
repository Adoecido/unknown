package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRenderTypeBuffer;
import vegas.sony.wrapper.Wrapper;

public class RenderItemFontBridge
extends Wrapper {

    public static RenderItemFontBridge V(WorldRenderer wg_12) {
        if (ForgeVersion.MC_1_21_0.d()) {
            return null;
        }
        return new RenderItemFontBridge(MRenderTypeBuffer.t(RenderItemFontBridge.sonyVegasInstance.getMappingsMapperCompat().c, wg_12.getObject()));
    }

    public void X() {
        MRenderTypeBuffer.Q(RenderItemFontBridge.sonyVegasInstance.getMappingsMapperCompat().c, this.I);
    }

    public RenderItemFontBridge(Object object) {
        super(object);
    }

    public void q() {
        MRenderTypeBuffer.n(RenderItemFontBridge.sonyVegasInstance.getMappingsMapperCompat().c, this.I);
    }
}

