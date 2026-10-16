package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class VoxelShape
extends Wrapper {
    public VoxelShape(Object handle) {
        super(handle);
    }

    public RenderItemFontBridge getBufferSource() {
        return new RenderItemFontBridge(VoxelShape.sonyVegasInstance.getMappingsMapperCompat().Cl.getBufferSource(this.I));
    }
}
