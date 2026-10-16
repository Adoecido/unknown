package vegas.sony.mapping.mappings;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MGlStateManager_BlendState
extends Mapping {
    public MappingField blendEnabledStateField;

    public Object getBlendEnabledState(Object blendStateHandle) {
        return this.blendEnabledStateField.getObject(blendStateHandle);
    }


    public MGlStateManager_BlendState() {
        this(MEntityRenderer.n());
    }

    private MGlStateManager_BlendState(int initializationState) {
        super(MappedClasses.Yk);
        if (initializationState != 0) {
            if (SonyVegas.INSTANCE.isVanillaMinecraftPresent()) {
                this.blendEnabledStateField = this.J("blend", true, MappedClasses.U);
            } else if (ForgeVersion.MC_1_20_6.d()) {
                this.blendEnabledStateField = this.fieldBuilder("mode", MappedClasses.U).buildField();
            } else {
                this.blendEnabledStateField = this.J("field_179213_a", Wrapper.isNativeAvailable, MappedClasses.U);
            }
            return;
        }
        if (SonyVegas.INSTANCE.isVanillaMinecraftPresent()) {
            this.blendEnabledStateField = this.fieldBuilder("mode", MappedClasses.U).buildField();
        }
        this.blendEnabledStateField = this.J("field_179213_a", Wrapper.isNativeAvailable, MappedClasses.U);
    }
}
