package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MRenderWorldLastEvent;
import vegas.sony.wrapper.Wrapper;

public class RenderWorldLastEvent
extends Wrapper {

    public static float getRenderResolutionMultiplier() {
        if (!SonyVegas.renderReady) {
            return 1.0f;
        }
        return MRenderWorldLastEvent.getRenderResolutionMultiplier(RenderWorldLastEvent.sonyVegasInstance.getMappingsMapperCompat().shadersConfig);
    }

    public RenderWorldLastEvent(Object handle) {
        super(handle);
    }
}

