package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBlockLayerOverrideFallback;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class BlockLayerOverrideFallbackEventMappingTask
extends JavassistMappingTask {
    public BlockLayerOverrideFallbackEventMappingTask() {
        super(MappedClasses.lA);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().N.gcMethod;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventBlockLayerOverrideFallback.class);
        eventInjectionSpec.setInsertBefore(true);
        this.registerEventInjection(eventInjectionSpec);
    }
}
