package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBlockModelRender;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class BlockModelRenderEventMappingTask
extends JavassistMappingTask {
    public BlockModelRenderEventMappingTask() {
        super(MappedClasses.VU);
    }

    @Override
    public void transform() {
        MappingMethod renderModelMethod = SonyVegas.INSTANCE.getMappings().hE.renderModelMethod;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(renderModelMethod, EventBlockModelRender.class);
        eventInjectionSpec.setConstructorArguments("$0, $1, $2, $3, $4, $5, $6");
        eventInjectionSpec.setReturnExpression("$event.getResult()");
        this.registerEventInjection(eventInjectionSpec);
    }
}
