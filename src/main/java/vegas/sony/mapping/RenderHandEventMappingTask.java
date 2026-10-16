package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPostRenderHand;
import vegas.sony.event.impl.EventPreRenderHand;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class RenderHandEventMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        if (SonyVegas.INSTANCE.getMappings().RY.x != null && !SonyVegas.INSTANCE.getMappings().RY.x.hasResolutionFailed()) {
            this.c(SonyVegas.INSTANCE.getMappings().RY.x, EventPreRenderHand.class, "");
            this.k(SonyVegas.INSTANCE.getMappings().RY.x, EventPostRenderHand.class, "");
        }
    }

    public RenderHandEventMappingTask() {
        super(MappedClasses.lt);
    }

}

