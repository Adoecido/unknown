package vegas.sony.mapping.mappings;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MEntityPlayerSP;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MAbstractClientPlayerBridge
extends Mapping {
    private MappingField clientAvatarStateField;
    private MappingMethod locationSkinMethod;
    private MappingMethod locationCapeMethod;
    private MappingMethod isWearingModelPartMethod;

    public Object getClientAvatarState(Object playerHandle) {
        return this.clientAvatarStateField.getObject(playerHandle);
    }

    public MAbstractClientPlayerBridge() {
        this(MEntityPlayerSP.r());
    }

    private MAbstractClientPlayerBridge(GuiComponent[] guiComponentArray) {
        super(MappedClasses.zt);
        if (guiComponentArray != null) {
            if (ForgeVersion.MC_1_21_10.d() && SonyVegas.INSTANCE.isFabricMinecraftPresent()) {
                this.locationSkinMethod = this.registerInstanceMethodForOwner(
                        MappedClasses.lB, "getSkin", true, MappedClasses.uZ, new Class[]{});
            } else {
                this.locationSkinMethod = this.Y("getLocationSkin", true, MappedClasses.zC, new Class[]{});
            }
            if (ForgeVersion.MC_1_21_10.d()) {
                this.clientAvatarStateField = this.J("clientAvatarState", true, MappedClasses.zT);
            }
            if (ForgeVersion.c() <= ForgeVersion.MC_1_16_5_ACTUAL.i()) {
                this.locationCapeMethod = this.Y("getLocationCape", true, MappedClasses.zC,
                        new Class[]{});
                this.isWearingModelPartMethod = this.registerInstanceMethodForOwner(
                        MappedClasses.Yl, "isWearing", true, Boolean.TYPE,
                        new Class[]{MappedClasses.PLAYER_MODEL_PART});
            }
            return;
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            this.locationSkinMethod = this.registerInstanceMethodForOwner(
                    MappedClasses.lB, "getSkin", true, MappedClasses.uZ, new Class[]{});
        }
        this.locationSkinMethod = this.Y("getLocationSkin", true, MappedClasses.zC, new Class[]{});
        if (ForgeVersion.MC_1_21_10.d()) {
            this.clientAvatarStateField = this.J("clientAvatarState", true, MappedClasses.zT);
        }
        if (ForgeVersion.c() <= ForgeVersion.MC_1_16_5_ACTUAL.i()) {
            this.locationCapeMethod = this.Y("getLocationCape", true, MappedClasses.zC,
                    new Class[]{});
            this.isWearingModelPartMethod = this.registerInstanceMethodForOwner(
                    MappedClasses.Yl, "isWearing", true, Boolean.TYPE,
                    new Class[]{MappedClasses.PLAYER_MODEL_PART});
        }
    }

    public Object getLocationSkin(Object playerHandle) {
        return this.locationSkinMethod.invokeObject(playerHandle, new Object[0]);
    }

    public MappingMethod getLocationCapeMethod() {
        return this.locationCapeMethod;
    }

    public MappingMethod getIsWearingModelPartMethod() {
        return this.isWearingModelPartMethod;
    }
}
