package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPostRenderWorldPass;
import vegas.sony.event.impl.EventPreRenderWorldPass;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class RenderWorldPassEventMappingTask
extends JavassistMappingTask {
    public RenderWorldPassEventMappingTask() {
        super(MappedClasses.zs);
    }

    @Override
    public void transform() {
        if (ForgeVersion.MC_1_21_4.d()) {
            try {
                MappingMethod mappingMethod = null;
                if (ForgeVersion.MC_1_21_10.d()) {
                    mappingMethod = SonyVegas.INSTANCE.getMappings().renderGlobal.extractVisibleEntitiesMethod;
                } else if (ForgeVersion.MC_1_21_4.d()) {
                    mappingMethod = SonyVegas.INSTANCE.getMappings().renderGlobal.renderEntitiesMethod;
                }
                this.c(mappingMethod, EventPreRenderWorldPass.class, "");
                this.k(mappingMethod, EventPostRenderWorldPass.class, "");
            }
            catch (Exception exception) {
                SonyVegas.logThrowable(exception);
            }
        }
    }
}
