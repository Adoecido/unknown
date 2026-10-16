package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MItem_ToolMaterial;
import vegas.sony.wrapper.Wrapper;

public class ToolMaterial
extends Wrapper {
    public float I() {
        return MItem_ToolMaterial.j(ToolMaterial.sonyVegasInstance.getMappings().Re, this.I);
    }

    public static ToolMaterial S() {
        return new ToolMaterial(MItem_ToolMaterial.H(ToolMaterial.sonyVegasInstance.getMappings().Re));
    }

    public static ToolMaterial u() {
        return new ToolMaterial(MItem_ToolMaterial.r(ToolMaterial.sonyVegasInstance.getMappings().Re));
    }

    public ToolMaterial(Object object) {
        super(object);
    }

    public static ToolMaterial f() {
        return new ToolMaterial(MItem_ToolMaterial.U(ToolMaterial.sonyVegasInstance.getMappings().Re));
    }

    public static ToolMaterial I$src$Lvegas_sony_wrapper_impl_ToolMaterial_$3t5lsk() {
        return new ToolMaterial(MItem_ToolMaterial.M(ToolMaterial.sonyVegasInstance.getMappings().Re));
    }
}

