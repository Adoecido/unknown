package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MLightTexture;
import vegas.sony.wrapper.Wrapper;

public class LightTexture
extends Wrapper {
    public void Z(float f) {
        MLightTexture.B(LightTexture.sonyVegasInstance.getMappingsMapperCompat().hF, this.I, f);
    }

    public void V() {
        if (ForgeVersion.MC_1_21_11.d()) {
            return;
        }
        MLightTexture.f(LightTexture.sonyVegasInstance.getMappingsMapperCompat().hF, this.I);
    }

    public LightTexture(Object object) {
        super(object);
    }


    public void X() {
        if (ForgeVersion.MC_1_21_11.d()) {
            return;
        }
        MLightTexture.z(LightTexture.sonyVegasInstance.getMappingsMapperCompat().hF, this.I);
    }
}

