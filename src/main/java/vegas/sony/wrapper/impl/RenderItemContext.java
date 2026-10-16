package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRenderItemContext;
import vegas.sony.wrapper.Wrapper;

public class RenderItemContext
extends Wrapper {
    public static RenderItemContext gui() {
        return new RenderItemContext(MRenderItemContext.getGui(RenderItemContext.sonyVegasInstance.getMappingsMapperCompat().A));
    }

    public RenderItemContext(Object wrappedObject) {
        super(wrappedObject);
    }
}
