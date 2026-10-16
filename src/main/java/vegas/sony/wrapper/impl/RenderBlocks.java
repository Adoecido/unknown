package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.wrapper.Wrapper;

public class RenderBlocks
extends Wrapper {
    public void setRenderAllFaces(boolean renderAllFaces) {
        SonyVegas.INSTANCE.getMappingsMapperCompat().renderBlocks.setRenderAllFaces(this.I, renderAllFaces);
    }

    public RenderBlocks(Object renderBlocksHandle) {
        super(renderBlocksHandle);
    }
}
