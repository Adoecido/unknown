package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;
import java.util.List;

public class Ordering
extends Wrapper {
    public Ordering(Object wrappedObject) {
        super(wrappedObject);
    }

    public List sortedCopy(Iterable iterable) {
        return Ordering.sonyVegasInstance.getMappingsMapperCompat().qm.sortedCopy(this.I, iterable);
    }
}
