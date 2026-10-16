package vegas.sony.wrapper.impl;

public class Direction
extends EnumFacing {
    public DirectionVector Q$src$Lvegas_sony_wrapper_impl_DirectionVector_$l2h44r() {
        return new DirectionVector(Direction.sonyVegasInstance.getMappings().Q.g(this.I));
    }

    public Direction(Object object) {
        super(object);
    }

    public DirectionAxis n() {
        return new DirectionAxis(Direction.sonyVegasInstance.getMappings().Q.J(this.I));
    }

    public int F() {
        return Direction.sonyVegasInstance.getMappings().Q.z(this.I);
    }

    public static Direction i(double d, double d2, double d3) {
        return new Direction(Direction.sonyVegasInstance.getMappings().Q.w(d, d2, d3));
    }

    public int Q() {
        return Direction.sonyVegasInstance.getMappings().Q.o(this.I);
    }

    public int S() {
        return Direction.sonyVegasInstance.getMappings().Q.S(this.I);
    }
}

