package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPlayerTabOverlayDisplayNameLegacy;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class PlayerTabOverlayDisplayNameLegacyMappingTask
extends JavassistMappingTask {
    public PlayerTabOverlayDisplayNameLegacyMappingTask() {
        super(MappedClasses.lF);
    }

    @Override
    public void transform() {
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(SonyVegas.INSTANCE.getMappings().hP.O, EventPlayerTabOverlayDisplayNameLegacy.class);
        eventInjectionSpec.setConstructorArguments("$0, $1");
        eventInjectionSpec.setReturnExpression("$event.getDisplayName()");
        this.registerEventInjection(eventInjectionSpec);
    }
}
