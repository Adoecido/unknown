package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SetVisibility
extends Wrapper {
    public SetVisibility(Object wrappedObject) {
        super(wrappedObject);
    }

    public void setAllVisible(boolean visible) {
        SetVisibility.sonyVegasInstance.getMappingsMapperCompat().setVisibility.setAllVisible(this.I, visible);
    }
}
