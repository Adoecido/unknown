package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MModelRenderer;
import vegas.sony.wrapper.Wrapper;

public class ModelRenderer
extends Wrapper {
    public float j() {
        return MModelRenderer.h(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public float s() {
        return MModelRenderer.k(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public void n(boolean bl) {
        MModelRenderer.r(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I, bl);
    }

    public ModelRenderer(Object object) {
        super(object);
    }

    public float M() {
        return MModelRenderer.i(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public float B() {
        return MModelRenderer.v(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public float q() {
        return MModelRenderer.b(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public float getRotateAngleY() {
        return MModelRenderer.s(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public float getRotateAngleZ() {
        return MModelRenderer.U(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public void S(boolean bl) {
        MModelRenderer.j(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I, bl);
    }

    public float getRotateAngleX() {
        return MModelRenderer.c(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public int s$src$I$x0ut69() {
        return MModelRenderer.z(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public float F() {
        return MModelRenderer.t(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }

    public int C() {
        return MModelRenderer.j(ModelRenderer.sonyVegasInstance.getMappings().Rz, this.I);
    }
}

