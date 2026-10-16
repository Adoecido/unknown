package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class EnumHand
extends Wrapper {
    public static EnumHand mainHand() {
        return new EnumHand(EnumHand.sonyVegasInstance.getMappingsMapperCompat().DX.getMainHand());
    }

    public static EnumHand offHand() {
        return new EnumHand(EnumHand.sonyVegasInstance.getMappingsMapperCompat().DX.getOffHand());
    }

    public EnumHand(Object wrappedObject) {
        super(wrappedObject);
    }
}
