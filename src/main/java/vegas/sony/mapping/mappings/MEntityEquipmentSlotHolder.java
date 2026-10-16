package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MEntityEquipmentSlotHolder
extends Mapping {
    private static final String SLOT_FIELD_NAME = "slot";
    private final MappingField slotField;

    public static Object getSlot(MEntityEquipmentSlotHolder mapping, Object holder) {
        return mapping.readSlot(holder);
    }

    public MEntityEquipmentSlotHolder() {
        super(MappedClasses.YW);
        this.slotField = this.J(SLOT_FIELD_NAME, true, MappedClasses.FY);
    }

    private Object readSlot(Object holder) {
        return this.slotField.getObject(holder);
    }
}

