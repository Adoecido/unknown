package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class BlockReaderBridge
extends Wrapper {
    public BlockReaderBridge(Object handle) {
        super(handle);
    }

    public EntityFishHook getShape(BlockReader blockReader, BlockPos blockPosition) {
        return new EntityFishHook(BlockReaderBridge.sonyVegasInstance.getMappingsMapperCompat().CJ.getShape(this.I, blockReader.getObject(), blockPosition.getObject()));
    }

    public boolean isSuffocating(Object blockReader, Object blockPosition) {
        return BlockReaderBridge.sonyVegasInstance.getMappingsMapperCompat().CJ.isSuffocating(this.I, blockReader, blockPosition);
    }
}
