package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventChunkRenderRebuild;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class ChunkRenderRebuildEventMappingTask
extends JavassistMappingTask {
    private static final String c = "($r) 1";

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().qg.w;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventChunkRenderRebuild.class);
        eventInjectionSpec.setReturnExpression(c);
        this.registerEventInjection(eventInjectionSpec);
    }

    public ChunkRenderRebuildEventMappingTask() {
        super(MappedClasses.Zk);
    }
}
