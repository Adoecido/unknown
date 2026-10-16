package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPlayerTabOverlayDisplayName;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class PlayerTabOverlayDisplayNameMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(SonyVegas.INSTANCE.getMappings().hP.O, EventPlayerTabOverlayDisplayName.class);
        eventInjectionSpec.setConstructorArguments("$0, $1");
        eventInjectionSpec.setReturnExpression("$event.getDisplayNameInstance()");
        this.registerEventInjection(eventInjectionSpec);
    }

    public PlayerTabOverlayDisplayNameMappingTask() {
        super(MappedClasses.lF);
    }
}
