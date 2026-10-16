package vegas.sony.mapping.mappings;

import vegas.sony.asm.helper.DescUtils;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MPointOfView
extends Mapping {
    private static final String VALUES_METHOD_NAME = "values";
    private final MappingMethod valuesMethod;

    public Object[] values() {
        return this.valuesMethod.invokeObjectArray(null, new Object[0]);
    }

    public MPointOfView() {
        super(MappedClasses.ZR);
        this.valuesMethod = this.registerStaticMethod(VALUES_METHOD_NAME, true, DescUtils.getArrayType(MappedClasses.ZR), new Class[]{});
    }
}

