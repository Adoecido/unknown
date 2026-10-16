package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRenderStateBridge;
import vegas.sony.wrapper.Wrapper;

public class RenderStateBridge
extends Wrapper {
    public double k() {
        return MRenderStateBridge.Z(RenderStateBridge.sonyVegasInstance.getMappingsMapperCompat().q3, this.I);
    }

    public ITextComponent d() {
        return new ITextComponent(RenderStateBridge.sonyVegasInstance.getMappingsMapperCompat().q3.u(this.I));
    }

    public double L() {
        return MRenderStateBridge.g(RenderStateBridge.sonyVegasInstance.getMappingsMapperCompat().q3, this.I);
    }

    public RenderStateBridge(Object object) {
        super(object);
    }

    public double F() {
        return MRenderStateBridge.Y(RenderStateBridge.sonyVegasInstance.getMappingsMapperCompat().q3, this.I);
    }

    public void Z(ITextComponent t3_02) {
        RenderStateBridge.sonyVegasInstance.getMappingsMapperCompat().q3.q(this.I, t3_02.getObject());
    }
}

