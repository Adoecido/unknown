package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MSoundEvent
extends Mapping {
    private static final String NAME_FIELD_NAME = "name";
    private final MappingField nameField;

    public MSoundEvent() {
        super(MappedClasses.Y6);
        this.nameField = this.J(NAME_FIELD_NAME, true, MappedClasses.zC);
    }

    public Object getName(Object soundEvent) {
        return this.nameField.getObject(soundEvent);
    }
}

