package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.EventPreRenderEntityForgeCallback;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class LegacyEntityRenderPreEventMappingTask
extends JavassistMappingTask {
    private static final String c = "$0, $1, $3, $4, $5";

    public LegacyEntityRenderPreEventMappingTask() {
        super(MappedClasses.x);
    }

    @Override
    public void transform() {
        this.k(SonyVegas.INSTANCE.getMappings().legacyEntityRenderPreHook.constructorMethod, EventPreRenderEntityForgeCallback.class, c);
    }
}
