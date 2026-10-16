package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MDirectionVector;
import vegas.sony.wrapper.Wrapper;

public class DirectionVector
extends Wrapper {
    public DirectionVector(Object wrappedObject) {
        super(wrappedObject);
    }

    public static DirectionVector positive() {
        return new DirectionVector(MDirectionVector.getPositive(DirectionVector.sonyVegasInstance.getMappings().qx));
    }
}
