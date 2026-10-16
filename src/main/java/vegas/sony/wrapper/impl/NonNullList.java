package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class NonNullList
extends Wrapper {
    public NonNullList(Object listHandle) {
        super(listHandle);
    }

    public static NonNullList create() {
        return new NonNullList(NonNullList.sonyVegasInstance.getMappingsMapperCompat().nonNullList.create());
    }
}
