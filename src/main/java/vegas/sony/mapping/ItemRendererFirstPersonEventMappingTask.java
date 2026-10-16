package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventRenderItemInFirstPerson;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class ItemRendererFirstPersonEventMappingTask
extends JavassistMappingTask {
    private static final String c = "$0, $1";

    @Override
    public void transform() {
        this.c(SonyVegas.INSTANCE.getMappings().qE.S, EventRenderItemInFirstPerson.class, c);
    }

    public ItemRendererFirstPersonEventMappingTask() {
        super(MappedClasses.zN);
    }
}
