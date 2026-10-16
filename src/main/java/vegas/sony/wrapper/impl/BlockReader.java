package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class BlockReader
extends Wrapper {
    public BlockReader(Object object) {
        super(object);
    }

    public BlockStatePredicate f(BlockPos blockPos) {
        return new BlockStatePredicate(BlockReader.sonyVegasInstance.getMappingsMapperCompat().y.getBlockState(this.I, blockPos.getObject()));
    }
}
