package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRayTraceContext_FluidMode;
import vegas.sony.wrapper.Wrapper;

public class RayTraceContext$BlockMode
extends Wrapper {
    public RayTraceContext$BlockMode(Object handle) {
        super(handle);
    }

    public static RayTraceContext$BlockMode any() {
        return new RayTraceContext$BlockMode(MRayTraceContext_FluidMode.getAny(RayTraceContext$BlockMode.sonyVegasInstance.getMappingsMapperCompat().rayTraceFluidMode));
    }

    public static RayTraceContext$BlockMode sourceOnly() {
        return new RayTraceContext$BlockMode(MRayTraceContext_FluidMode.getSourceOnly(RayTraceContext$BlockMode.sonyVegasInstance.getMappingsMapperCompat().rayTraceFluidMode));
    }

    public static RayTraceContext$BlockMode none() {
        return new RayTraceContext$BlockMode(MRayTraceContext_FluidMode.getNone(RayTraceContext$BlockMode.sonyVegasInstance.getMappingsMapperCompat().rayTraceFluidMode));
    }
}
