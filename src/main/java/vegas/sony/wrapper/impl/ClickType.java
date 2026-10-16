package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MClickType;
import vegas.sony.wrapper.Wrapper;

public class ClickType
extends Wrapper {
    public static ClickType[] VALUES = new ClickType[]{ClickType.pickup(), ClickType.quickMove(), ClickType.swap(), ClickType.cloneStack(), ClickType.throwStack(), ClickType.quickCraft(), ClickType.pickupAll()};

    public static ClickType quickCraft() {
        return new ClickType(MClickType.getQuickCraft(ClickType.sonyVegasInstance.getMappingsMapperCompat().clickType));
    }

    public ClickType(Object object) {
        super(object);
    }

    public static ClickType pickup() {
        return new ClickType(MClickType.getPickup(ClickType.sonyVegasInstance.getMappingsMapperCompat().clickType));
    }

    public static ClickType cloneStack() {
        return new ClickType(MClickType.getClone(ClickType.sonyVegasInstance.getMappingsMapperCompat().clickType));
    }

    public static ClickType quickMove() {
        return new ClickType(MClickType.getQuickMove(ClickType.sonyVegasInstance.getMappingsMapperCompat().clickType));
    }

    public static ClickType pickupAll() {
        return new ClickType(MClickType.getPickupAll(ClickType.sonyVegasInstance.getMappingsMapperCompat().clickType));
    }

    public static ClickType throwStack() {
        return new ClickType(MClickType.getThrow(ClickType.sonyVegasInstance.getMappingsMapperCompat().clickType));
    }

    public static ClickType swap() {
        return new ClickType(MClickType.getSwap(ClickType.sonyVegasInstance.getMappingsMapperCompat().clickType));
    }
}
