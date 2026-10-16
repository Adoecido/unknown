package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBlockRenderColorOpacity;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class BlockRenderColorOpacityMappingTask
extends JavassistMappingTask {
    private static final String c = "$0, $1, $2, $3, $4";

    public BlockRenderColorOpacityMappingTask() {
        super(MappedClasses.lX);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().qZ.b;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventBlockRenderColorOpacity.class);
        eventInjectionSpec.setInsertBefore(false);
        eventInjectionSpec.setConstructorArguments(c);
        this.registerEventInjection(eventInjectionSpec);
    }
}
