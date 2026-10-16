package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class GlStateManagerTexGenCoord
extends Wrapper {
    public GlStateManagerTexGenCoord(Object coordinateHandle) {
        super(coordinateHandle);
    }

    public static GlStateManagerTexGenCoord none() {
        return new GlStateManagerTexGenCoord(
                GlStateManagerTexGenCoord.sonyVegasInstance.getMappingsMapperCompat().glTexGenCoord.getNone());
    }

    public static GlStateManagerTexGenCoord server() {
        return new GlStateManagerTexGenCoord(
                GlStateManagerTexGenCoord.sonyVegasInstance.getMappingsMapperCompat().glTexGenCoord.getServer());
    }

    public static GlStateManagerTexGenCoord client() {
        return new GlStateManagerTexGenCoord(
                GlStateManagerTexGenCoord.sonyVegasInstance.getMappingsMapperCompat().glTexGenCoord.getClient());
    }
}
