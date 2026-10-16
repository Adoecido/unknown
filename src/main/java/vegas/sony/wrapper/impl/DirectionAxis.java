package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MDirectionAxis;
import vegas.sony.wrapper.Wrapper;

public class DirectionAxis
extends Wrapper {
    public static DirectionAxis x() {
        return new DirectionAxis(MDirectionAxis.getX(DirectionAxis.sonyVegasInstance.getMappingsMapperCompat().directionAxis));
    }

    public static DirectionAxis y() {
        return new DirectionAxis(MDirectionAxis.getY(DirectionAxis.sonyVegasInstance.getMappingsMapperCompat().directionAxis));
    }

    public DirectionAxis(Object handle) {
        super(handle);
    }

    public double choose(double x, double y, double z) {
        return DirectionAxis.sonyVegasInstance.getMappingsMapperCompat().directionAxis.choose(this.I, x, y, z);
    }
}
