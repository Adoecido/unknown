package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MEnumCreatureAttributeBridge
extends Mapping {
    private static final String UNDEFINED_FIELD_NAME = "UNDEFINED";
    private final MappingField undefinedField;

    public MEnumCreatureAttributeBridge() {
        super(MappedClasses.O);
        this.undefinedField = this.registerStaticField(UNDEFINED_FIELD_NAME, true, MappedClasses.O);
    }

    public Object getUndefined() {
        return this.undefinedField.getObject(null);
    }
}

