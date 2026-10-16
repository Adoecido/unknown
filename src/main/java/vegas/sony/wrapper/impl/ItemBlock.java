package vegas.sony.wrapper.impl;

public class ItemBlock
extends Item {
    public Block C() {
        return new Block(ItemBlock.sonyVegasInstance.getMappingsMapperCompat().hb.getBlock(this.I));
    }

    public ItemBlock(Object object) {
        super(object);
    }
}
