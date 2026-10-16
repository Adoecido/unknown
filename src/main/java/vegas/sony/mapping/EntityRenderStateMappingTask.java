package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventEntityRenderState;
import vegas.sony.mapping.EventPreRenderEntityCallback;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class EntityRenderStateMappingTask
extends JavassistMappingTask {

    @Override
    public void transform() {
        if (!ForgeVersion.MC_1_7_10.L()) {
            MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().qe.p;
            if (ForgeVersion.MC_1_21_10.d()) {
                MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().qe.z;
                this.k(mappingMethod2, EventEntityRenderState.class, "$1, $2");
                return;
            }
            if (ForgeVersion.MC_1_21_0.d()) {
                return;
            }
            if (ForgeVersion.MC_1_16_5.d()) {
                this.c(mappingMethod, EventPreRenderEntityCallback.class, "$1, $4");
                return;
            }
            if (!mappingMethod.hasResolutionFailed()) {
                this.c(mappingMethod, EventPreRenderEntityCallback.class, "$1, $2, $3, $4");
            }
        }
    }

    public EntityRenderStateMappingTask() {
        super(MappedClasses.VQ);
    }
}

