package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingFieldBuilder;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MResourceKey
extends Mapping {
    private final MappingField locationField;

    public MResourceKey() {
        super(MappedClasses.qB);
        this.locationField = ((MappingFieldBuilder)this.fieldBuilder("location", MappedClasses.zC).setNameForVersion(ForgeVersion.MC_1_21_11.n(), "identifier")).buildField();
    }

    public static Object getLocation(MResourceKey mapping, Object resourceKey) {
        return mapping.readLocation(resourceKey);
    }

    private Object readLocation(Object resourceKey) {
        return this.locationField.getObject(resourceKey);
    }
}

