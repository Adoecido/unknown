package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEntityEquipmentSlot;
import vegas.sony.wrapper.Wrapper;

public class EntityEquipmentSlot
extends Wrapper {
    public static EntityEquipmentSlot L() {
        return new EntityEquipmentSlot(MEntityEquipmentSlot.Q(EntityEquipmentSlot.sonyVegasInstance.getMappingsMapperCompat().CY));
    }

    public boolean v() {
        return MEntityEquipmentSlot.S(EntityEquipmentSlot.sonyVegasInstance.getMappingsMapperCompat().CY, this.I);
    }

    public EntityEquipmentSlot(Object object) {
        super(object);
    }

    public int W() {
        return MEntityEquipmentSlot.c(EntityEquipmentSlot.sonyVegasInstance.getMappingsMapperCompat().CY, this.I);
    }
}

