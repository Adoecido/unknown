package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MEnumActionResult
extends Mapping {
    public final MappingField passField;
    public final MappingField successField;
    public final MappingField failField;

    public Object getPass() {
        return this.passField.getObject(null);
    }

    public MEnumActionResult() {
        super(MappedClasses.zr);
        this.successField = this.registerStaticField("SUCCESS", true, MappedClasses.zr);
        this.passField = this.registerStaticField("PASS", true, MappedClasses.zr);
        this.failField = this.registerStaticField("FAIL", true, MappedClasses.zr);
    }

    public Object getFail() {
        return this.failField.getObject(null);
    }

    public Object getSuccess() {
        return this.successField.getObject(null);
    }
}

