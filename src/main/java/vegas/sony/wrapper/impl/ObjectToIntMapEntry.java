package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ObjectToIntMapEntry
extends Wrapper {
    public int getIntValue() {
        return ObjectToIntMapEntry.sonyVegasInstance.getMappings().objectToIntMapEntry.getIntValue(this.I);
    }

    public ObjectToIntMapEntry(Object wrappedObject) {
        super(wrappedObject);
    }

    public Object getKey() {
        return ObjectToIntMapEntry.sonyVegasInstance.getMappings().objectToIntMapEntry.getKey(this.I);
    }
}
