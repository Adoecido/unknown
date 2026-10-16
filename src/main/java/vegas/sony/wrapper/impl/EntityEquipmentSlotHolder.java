package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEntityEquipmentSlotHolder;
import vegas.sony.wrapper.Wrapper;

public class EntityEquipmentSlotHolder
extends Wrapper {
    public EntityEquipmentSlot getSlot() {
        return new EntityEquipmentSlot(MEntityEquipmentSlotHolder.getSlot(EntityEquipmentSlotHolder.sonyVegasInstance.getMappingsMapperCompat().hh, this.I));
    }

    public EntityEquipmentSlotHolder(Object wrappedObject) {
        super(wrappedObject);
    }
}
