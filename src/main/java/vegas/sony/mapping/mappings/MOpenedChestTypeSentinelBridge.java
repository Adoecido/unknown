package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MOpenedChestTypeSentinelBridge
extends Mapping {
    private static final String BASIC_FIELD_NAME = "BASIC";
    private final MappingField basicField;

    public MOpenedChestTypeSentinelBridge() {
        super(MappedClasses.q1);
        this.basicField = this.registerStaticField(BASIC_FIELD_NAME, true, MappedClasses.q1);
    }

    public Object getBasic() {
        return this.basicField.getObject(null);
    }
}

