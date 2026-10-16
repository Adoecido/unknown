package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MBiomeProviderBridge
extends Mapping {
    private static final String GET_ALL_EFFECTS_METHOD_NAME = "getAllEffects";
    private final MappingMethod getAllEffectsMethod;

    public static Iterable getAllEffects(MBiomeProviderBridge mapping, Object biomeProvider) {
        return mapping.invokeGetAllEffects(biomeProvider);
    }

    public MBiomeProviderBridge() {
        super(MappedClasses.uV);
        this.getAllEffectsMethod = this.Y(GET_ALL_EFFECTS_METHOD_NAME, true, Iterable.class, new Class[]{});
    }

    private Iterable invokeGetAllEffects(Object biomeProvider) {
        return (Iterable)this.getAllEffectsMethod.invokeObject(biomeProvider, new Object[0]);
    }
}

