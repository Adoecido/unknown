package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MChestType
extends Mapping {
    private static final String FALL_DAMAGE_RESETTING_FIELD_NAME = "FALLDAMAGE_RESETTING";
    private final MappingField fallDamageResettingField;

    public MChestType() {
        super(MappedClasses.lN);
        this.fallDamageResettingField = this.registerStaticField(FALL_DAMAGE_RESETTING_FIELD_NAME, true, MappedClasses.lN);
    }

    public Object getFallDamageResetting() {
        return this.fallDamageResettingField.getObject(null);
    }
}

