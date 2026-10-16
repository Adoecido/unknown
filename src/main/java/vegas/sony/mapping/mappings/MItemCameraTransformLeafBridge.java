package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MItemCameraTransformLeafBridge
extends Mapping {
    private static final String KEY_FIELD_NAME = "key";
    private final MappingField keyField;

    public Object getKey(Object transform) {
        return this.keyField.getObject(transform);
    }

    public MItemCameraTransformLeafBridge() {
        super(MappedClasses.zo);
        this.keyField = this.J(KEY_FIELD_NAME, true, MappedClasses.qC);
    }
}

