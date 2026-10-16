package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPreRenderEntity;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class RenderManagerEntityMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        MappingMethod mappingMethod;
        if (ForgeVersion.MC_1_8_9.L() && !(mappingMethod = SonyVegas.INSTANCE.getMappings().CA.W).hasResolutionFailed()) {
            this.O(mappingMethod, EventPreRenderEntity.class, "$1", "false");
        }
        if (ForgeVersion.MC_1_16_5.d() && !(mappingMethod = SonyVegas.INSTANCE.getMappings().CA.W).hasResolutionFailed()) {
            this.c(mappingMethod, EventPreRenderEntity.class, "$1");
        }
    }


    public RenderManagerEntityMappingTask() {
        super(MappedClasses.Dc);
    }
}

