package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class BlockStateWorldBridge
extends Wrapper {
    public boolean isTag(Object tag) {
        return BlockStateWorldBridge.sonyVegasInstance.getMappingsMapperCompat().hu.isTag(this.getObject(), tag);
    }

    public float getHeight(World world, BlockPos blockPosition) {
        return BlockStateWorldBridge.sonyVegasInstance.getMappingsMapperCompat().hu.getHeight(this.getObject(), world.getObject(), blockPosition.getObject());
    }

    public BlockStateWorldBridge(Object handle) {
        super(handle);
    }

    public AbstractBlockState getType() {
        return new AbstractBlockState(BlockStateWorldBridge.sonyVegasInstance.getMappingsMapperCompat().hu.getType(this.I));
    }

    public boolean isEmpty() {
        return BlockStateWorldBridge.sonyVegasInstance.getMappingsMapperCompat().hu.isEmpty(this.I);
    }
}
