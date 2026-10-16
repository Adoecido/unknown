package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class RenderStateBuilder
extends Wrapper {
    public RenderStateBuilder(Object builderHandle) {
        super(builderHandle);
    }

    public static void drawWithShader(RenderState renderState) {
        RenderStateBuilder.sonyVegasInstance.getMappingsMapperCompat().renderStateBuilder
                .drawWithShader(renderState.getObject());
    }
}
