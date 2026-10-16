package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MSharedMonsterAttributesVariantBridge
extends Mapping {
    private final MappingField noneField;
    private final MappingField allField;

    public MSharedMonsterAttributesVariantBridge() {
        super(MappedClasses.Vx);
        this.noneField = this.registerStaticField("NONE", true, MappedClasses.Vx);
        this.allField = this.registerStaticField("ALL", true, MappedClasses.Vx);
    }

    public Object getNone() {
        return this.noneField.getObject(null);
    }

    public Object getAll() {
        return this.allField.getObject(null);
    }
}
