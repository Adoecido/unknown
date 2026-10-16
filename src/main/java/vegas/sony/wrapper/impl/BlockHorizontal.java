package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MBlockHorizontal;

public class BlockHorizontal
extends Block {
    public BlockHorizontal(Object wrappedObject) {
        super(wrappedObject);
    }

    public static BlockProperty facing() {
        return new BlockProperty(MBlockHorizontal.getFacing(BlockHorizontal.sonyVegasInstance.getMappingsMapperCompat().a));
    }
}
