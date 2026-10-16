package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBlockLayerOverride;
import vegas.sony.event.impl.EventBlockRenderLayerGate;
import vegas.sony.event.impl.EventBlockShouldRender;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class BlockRenderLayerEventMappingTask
extends JavassistMappingTask {
    public BlockRenderLayerEventMappingTask() {
        super(MappedClasses.Zk);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().qg.J;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventBlockShouldRender.class);
        eventInjectionSpec.setConstructorArguments("$0");
        eventInjectionSpec.setReturnExpression("true");
        this.registerEventInjection(eventInjectionSpec);
        MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().qg.S;
        EventInjectionSpec eventInjectionSpec2 = new EventInjectionSpec(mappingMethod2, EventBlockLayerOverride.class);
        eventInjectionSpec2.setConstructorArguments("$0");
        eventInjectionSpec2.setReturnExpression("($r) $event.getBlockLayer()");
        this.registerEventInjection(eventInjectionSpec2);
        MappingMethod mappingMethod3 = SonyVegas.INSTANCE.getMappings().qg.M;
        EventInjectionSpec eventInjectionSpec3 = new EventInjectionSpec(mappingMethod3, EventBlockRenderLayerGate.class);
        eventInjectionSpec3.setInsertBefore(true);
        eventInjectionSpec3.setReturnExpression("($r) 1");
        this.registerEventInjection(eventInjectionSpec3);
    }
}
