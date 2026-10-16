package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEquipmentSlotSet;
import vegas.sony.wrapper.Wrapper;

public class EquipmentSlotSet
extends Wrapper {
    public int d() {
        return MEquipmentSlotSet.e(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro, this.I);
    }

    public static EquipmentSlotSet n() {
        return new EquipmentSlotSet(MEquipmentSlotSet.e(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro));
    }

    public static EquipmentSlotSet T(int slot) {
        switch (slot) {
            case 0:
                return EquipmentSlotSet.k();
            case 1:
                return EquipmentSlotSet.q();
            case 2:
                return EquipmentSlotSet.o();
            case 3:
                return EquipmentSlotSet.n();
            case 4:
                return EquipmentSlotSet.j();
            case 5:
                return EquipmentSlotSet.U();
            default:
                return null;
        }
    }

    public static EquipmentSlotSet q() {
        return new EquipmentSlotSet(MEquipmentSlotSet.P(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro));
    }

    public static EquipmentSlotSet U() {
        return new EquipmentSlotSet(MEquipmentSlotSet.T(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro));
    }

    public static EquipmentSlotSet o() {
        return new EquipmentSlotSet(MEquipmentSlotSet.s(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro));
    }


    public static EquipmentSlotSet k() {
        return new EquipmentSlotSet(MEquipmentSlotSet.r(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro));
    }

    public MappedFieldSingletonWrapper y() {
        return new MappedFieldSingletonWrapper(MEquipmentSlotSet.a(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro, this.I));
    }

    public static EquipmentSlotSet j() {
        return new EquipmentSlotSet(MEquipmentSlotSet.t(EquipmentSlotSet.sonyVegasInstance.getMappingsMapperCompat().Ro));
    }

    public EquipmentSlotSet(Object object) {
        super(object);
    }
}
