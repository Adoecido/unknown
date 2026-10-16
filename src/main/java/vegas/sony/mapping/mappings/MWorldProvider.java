package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MWorldProvider
extends Mapping {
    private static final String GET_HAS_NO_SKY_METHOD_NAME = "getHasNoSky";
    private final MappingMethod getHasNoSkyMethod;

    public MWorldProvider() {
        super(MappedClasses.WORLD_PROVIDER);
        this.getHasNoSkyMethod = this.Y(GET_HAS_NO_SKY_METHOD_NAME, true, Boolean.TYPE, new Class[]{});
    }

    public boolean hasNoSky(Object worldProvider) {
        return this.getHasNoSkyMethod.invokeBoolean(worldProvider, new Object[0]);
    }
}
