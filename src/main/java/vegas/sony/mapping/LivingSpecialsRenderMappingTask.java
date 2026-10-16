package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPreRenderLivingSpecials;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.ForgeVersion;

public class LivingSpecialsRenderMappingTask
extends JavassistMappingTask {
    public LivingSpecialsRenderMappingTask() {
        super(MappedClasses.uP);
    }


    @Override
    public void transform() {
        if (ForgeVersion.c() == ForgeVersion.MC_1_8_9.i()) {
            this.O(SonyVegas.INSTANCE.getMappings().U.V, EventPreRenderLivingSpecials.class, "$1", "");
            return;
        }
        if (ForgeVersion.c() != ForgeVersion.MC_1_21_11.i()) {
            return;
        }
        this.O(SonyVegas.INSTANCE.getMappings().U.d, EventPreRenderLivingSpecials.class, "$2", "0");
    }
}

