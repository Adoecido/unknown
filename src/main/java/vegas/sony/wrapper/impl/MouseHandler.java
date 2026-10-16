package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MInputMappings;
import vegas.sony.wrapper.Wrapper;

public class MouseHandler
extends Wrapper {
    public void O() {
        MInputMappings.F$src$V$13evi63(MouseHandler.sonyVegasInstance.getMappings().CW, this.I);
    }

    public double R() {
        return MInputMappings.g(MouseHandler.sonyVegasInstance.getMappings().CW, this.I);
    }

    public MouseHandler(Object object) {
        super(object);
    }

    public void L(long l, int n, boolean bl) {
        MInputMappings.d(MouseHandler.sonyVegasInstance.getMappings().CW, this.I, l, n, bl);
    }

    public void u() {
        MInputMappings.V(MouseHandler.sonyVegasInstance.getMappings().CW, this.I);
    }

    public int d() {
        return MInputMappings.T(MouseHandler.sonyVegasInstance.getMappings().CW, this.I);
    }

    public int z() {
        return MInputMappings.F(MouseHandler.sonyVegasInstance.getMappings().CW, this.I);
    }

    public boolean Z() {
        return MInputMappings.w(MouseHandler.sonyVegasInstance.getMappings().CW, this.I);
    }

    public double b() {
        return MInputMappings.B(MouseHandler.sonyVegasInstance.getMappings().CW, this.I);
    }
}

