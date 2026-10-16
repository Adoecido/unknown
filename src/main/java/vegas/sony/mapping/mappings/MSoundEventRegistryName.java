package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MSoundEventRegistryName
extends Mapping {
    private static final String NAME_FIELD_NAME = "name";
    private final MappingField nameField;

    public static Object getName(MSoundEventRegistryName mapping, Object soundEvent) {
        return mapping.readName(soundEvent);
    }

    private Object readName(Object soundEvent) {
        return this.nameField.getObject(soundEvent);
    }

    public MSoundEventRegistryName() {
        super(MappedClasses.V4);
        this.nameField = this.J(NAME_FIELD_NAME, true, MappedClasses.zC);
    }
}

