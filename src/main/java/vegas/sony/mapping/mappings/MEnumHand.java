package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.wrapper.Wrapper;

public class MEnumHand
extends Mapping {
    private final MappingField offHandField;
    private final MappingField mainHandField;

    public MEnumHand() {
        super(MappedClasses.Yf);
        this.mainHandField = this.registerStaticField("MAIN_HAND", Wrapper.isNativeAvailable, MappedClasses.Yf);
        this.offHandField = this.registerStaticField("OFF_HAND", Wrapper.isNativeAvailable, MappedClasses.Yf);
    }

    public Object getOffHand() {
        return this.offHandField.getObject(null);
    }

    public Object getMainHand() {
        return this.mainHandField.getObject(null);
    }
}

