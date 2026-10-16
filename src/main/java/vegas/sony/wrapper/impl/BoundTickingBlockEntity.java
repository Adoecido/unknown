package vegas.sony.wrapper.impl;

public class BoundTickingBlockEntity
extends TickingBlockEntity {
    public BoundTickingBlockEntity(Object wrappedObject) {
        super(wrappedObject);
    }

    public Object getBlockEntity() {
        return BoundTickingBlockEntity.sonyVegasInstance.getMappingsMapperCompat().boundTickingBlockEntity.getBlockEntity(this.I);
    }
}
