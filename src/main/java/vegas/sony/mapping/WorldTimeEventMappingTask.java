package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventWorldTime;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class WorldTimeEventMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(SonyVegas.INSTANCE.getMappings().worldInfo.getWorldTimeMethod, EventWorldTime.class);
        eventInjectionSpec.setConstructorArguments("$0");
        eventInjectionSpec.setReturnExpression("$event.getWorldTime()");
        this.registerEventInjection(eventInjectionSpec);
    }

    public WorldTimeEventMappingTask() {
        super(MappedClasses.WORLD_INFO);
    }
}
