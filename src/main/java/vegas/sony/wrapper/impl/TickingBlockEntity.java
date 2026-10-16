package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTickingBlockEntity;
import vegas.sony.wrapper.Wrapper;

public class TickingBlockEntity
extends Wrapper {
    public BlockPos getPos() {
        return new BlockPos(MTickingBlockEntity.getPos(TickingBlockEntity.sonyVegasInstance.getMappingsMapperCompat().tickingBlockEntity, this.I));
    }

    public TickingBlockEntity(Object wrappedObject) {
        super(wrappedObject);
    }
}
