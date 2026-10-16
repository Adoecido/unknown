package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ItemAttributeModifiersComponent$Entry
extends Wrapper {
    public EquipmentSlotGroup getEquipmentSlotGroup() {
        Holder attributeHolder = new Holder(ItemAttributeModifiersComponent$Entry.sonyVegasInstance.getMappingsMapperCompat().DN.getAttribute(this.I));
        return new EquipmentSlotGroup(attributeHolder.N());
    }

    public AttributeModifier getModifier() {
        return new AttributeModifier(ItemAttributeModifiersComponent$Entry.sonyVegasInstance.getMappingsMapperCompat().DN.getModifier(this.I));
    }

    public ItemAttributeModifiersComponent$Entry(Object wrappedObject) {
        super(wrappedObject);
    }
}
