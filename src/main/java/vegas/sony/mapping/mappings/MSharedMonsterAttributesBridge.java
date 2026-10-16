package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MSharedMonsterAttributesBridge
extends Mapping {
    private final MappingField normalField;
    private final MappingField seeThroughField;

    public MSharedMonsterAttributesBridge() {
        super(MappedClasses.Y8);
        this.normalField = this.registerStaticField("NORMAL", true, MappedClasses.Y8);
        this.seeThroughField = this.registerStaticField("SEE_THROUGH", true, MappedClasses.Y8);
    }

    public Object getNormal() {
        return this.normalField.getObject(null);
    }

    public Object getSeeThrough() {
        return this.seeThroughField.getObject(null);
    }
}
