package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MEntity;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MEnderPearlProjectileBridge
extends Mapping {
    private MappingMethod getOwnerMethod;

    public MEnderPearlProjectileBridge() {
        this(MEntity.P());
    }

    private MEnderPearlProjectileBridge(int entityControlFlowState) {
        super(MappedClasses.lv);
        if (entityControlFlowState != 0) {
            if (ForgeVersion.MC_1_21_4.d()) {
                this.getOwnerMethod = this.Y("getOwner", true, MappedClasses.zc);
            }
            return;
        }
        if (ForgeVersion.MC_1_21_4.d()) {
            this.getOwnerMethod = this.registerInstanceMethodForOwner(
                    MappedClasses.YV, "getOwner", true, MappedClasses.zc);
        } else if (ForgeVersion.MC_1_16_5.d()) {
            this.getOwnerMethod = this.Y("getOwner", true, MappedClasses.zc);
        }
    }

    public Object getOwner(Object projectileHandle) {
        return this.getOwnerMethod.invokeObject(projectileHandle);
    }

}
