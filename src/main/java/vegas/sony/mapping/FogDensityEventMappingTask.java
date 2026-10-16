package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventFogDensity;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import javassist.CtBehavior;

public class FogDensityEventMappingTask
extends JavassistMappingTask {
    private static final String c = "0.1F";

    @Override
    public void transform() {
        CtBehavior ctBehavior = this.F(SonyVegas.INSTANCE.getMappings().fogRenderer.setupFogMethod);
        this.H(ctBehavior, EventFogDensity.class, c, "", "");
    }

    public FogDensityEventMappingTask() {
        super(MappedClasses.FOG_RENDERER);
    }
}
