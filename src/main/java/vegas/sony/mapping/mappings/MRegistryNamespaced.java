package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MappingMethodBuilder;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MRegistryNamespaced
extends Mapping {
    private final MappingMethod getByValueMethod;

    public MRegistryNamespaced() {
        super(MappedClasses.lz);
        this.getByValueMethod = ((MappingMethodBuilder)((MappingMethodBuilder)this.methodBuilder("getByValue", Object.class, new Class[]{Integer.TYPE}).setNameForVersion(ForgeVersion.MC_1_16_5.n(), "byId")).setOwnerClassForVersion(ForgeVersion.MC_1_21_4.n(), MappedClasses.ua)).buildMethod();
    }

    public Object getByValue(Object registry, int id) {
        return this.getByValueMethod.invokeObject(registry, id);
    }
}

