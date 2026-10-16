package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBlockLayerOverrideModern;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class BlockLayerOverrideModernEventMappingTask
extends JavassistMappingTask {
    public BlockLayerOverrideModernEventMappingTask() {
        super(MappedClasses.qa);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().qg.S;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventBlockLayerOverrideModern.class);
        eventInjectionSpec.setConstructorArguments("$0");
        eventInjectionSpec.setReturnExpression("($r) $event.getBlockLayer()");
        this.registerEventInjection(eventInjectionSpec);
    }
}
