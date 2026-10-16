package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class FogType
extends Wrapper {
    public static FogType noneOrSky() {
        return new FogType(FogType.sonyVegasInstance.getMappingsMapperCompat().fogType.getNoneOrSky());
    }

    public FogType(Object wrappedObject) {
        super(wrappedObject);
    }

    public static FogType terrainOrWorld() {
        return new FogType(FogType.sonyVegasInstance.getMappingsMapperCompat().fogType.getTerrainOrWorld());
    }
}
