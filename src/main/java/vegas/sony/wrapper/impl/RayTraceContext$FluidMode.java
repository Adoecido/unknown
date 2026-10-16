package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRayTraceContext_BlockMode;
import vegas.sony.wrapper.Wrapper;

public class RayTraceContext$FluidMode
extends Wrapper {
    public static RayTraceContext$FluidMode visual() {
        return new RayTraceContext$FluidMode(MRayTraceContext_BlockMode.getVisual(RayTraceContext$FluidMode.sonyVegasInstance.getMappingsMapperCompat().rayTraceBlockMode));
    }

    public RayTraceContext$FluidMode(Object handle) {
        super(handle);
    }

    public static RayTraceContext$FluidMode outline() {
        return new RayTraceContext$FluidMode(MRayTraceContext_BlockMode.getOutline(RayTraceContext$FluidMode.sonyVegasInstance.getMappingsMapperCompat().rayTraceBlockMode));
    }

    public static RayTraceContext$FluidMode collider() {
        return new RayTraceContext$FluidMode(MRayTraceContext_BlockMode.getCollider(RayTraceContext$FluidMode.sonyVegasInstance.getMappingsMapperCompat().rayTraceBlockMode));
    }
}
