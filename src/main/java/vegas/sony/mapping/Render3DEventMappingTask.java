package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.EventRender3DCallback;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class Render3DEventMappingTask
extends JavassistMappingTask {
    private static final String c = "$1";

    public Render3DEventMappingTask() {
        super(MappedClasses.zs);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().renderGlobal.renderLevelMethod;
        this.k(mappingMethod, EventRender3DCallback.class, c);
    }
}
