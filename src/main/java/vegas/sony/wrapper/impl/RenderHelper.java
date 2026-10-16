package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRenderHelper;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.wrapper.Wrapper;

public class RenderHelper
extends Wrapper {
    public RenderHelper(Object object) {
        super(object);
    }


    public static void s() {
        if (ForgeVersion.MC_1_17.d()) {
            return;
        }
        MRenderHelper.L(RenderHelper.sonyVegasInstance.getMappingsMapperCompat().x);
    }

    public static void l() {
        if (ForgeVersion.MC_1_17.d()) {
            MRenderHelper.M(RenderHelper.sonyVegasInstance.getMappingsMapperCompat().x);
            return;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            OpenGlBackendHolder.backend.pushMatrix();
            OpenGlBackendHolder.backend.rotate(-30.0f, 0.0f, 1.0f, 0.0f);
            OpenGlBackendHolder.backend.rotate(165.0f, 1.0f, 0.0f, 0.0f);
            RenderHelper.e();
            OpenGlBackendHolder.backend.popMatrix();
            return;
        }
        MRenderHelper.M(RenderHelper.sonyVegasInstance.getMappingsMapperCompat().x);
    }

    public static void e() {
        if (ForgeVersion.MC_1_17.d()) {
            return;
        }
        MRenderHelper.Q(RenderHelper.sonyVegasInstance.getMappingsMapperCompat().x);
    }
}

