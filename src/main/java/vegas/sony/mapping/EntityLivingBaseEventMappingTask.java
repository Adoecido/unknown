package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventEntityRendererRayTrace;
import vegas.sony.event.impl.EventPostEntityUpdate;
import vegas.sony.event.impl.EventPostLivingTravel;
import vegas.sony.event.impl.EventPotionEffectCheck;
import vegas.sony.event.impl.EventPreEntityUpdate;
import vegas.sony.event.impl.EventPreLivingTravel;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class EntityLivingBaseEventMappingTask
extends JavassistMappingTask {
    public EntityLivingBaseEventMappingTask() {
        super(MappedClasses.zm);
    }


    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Rr.V;
        this.c(mappingMethod, EventPreEntityUpdate.class, "$0");
        this.k(mappingMethod, EventPostEntityUpdate.class, "$0");
        MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().hx.U;
        this.c(mappingMethod2, EventPreLivingTravel.class, "$0");
        this.k(mappingMethod2, EventPostLivingTravel.class, "$0");
        if (ForgeVersion.MC_1_8_9.L()) {
            EventInjectionSpec eventInjectionSpec;
            if (!SonyVegas.INSTANCE.isLabyModPresent()) {
                eventInjectionSpec = new EventInjectionSpec(SonyVegas.INSTANCE.getMappings().Rr.jL, EventEntityRendererRayTrace.class);
                eventInjectionSpec.setConstructorArguments("$0, $1");
                eventInjectionSpec.setReturnExpression("($r) $event.getVec();");
                this.registerEventInjection(eventInjectionSpec);
            }
            eventInjectionSpec = new EventInjectionSpec(SonyVegas.INSTANCE.getMappings().hx.s, EventPotionEffectCheck.class);
            eventInjectionSpec.setConstructorArguments("$0, $1");
            eventInjectionSpec.setReturnExpression("($r) $event.isActive();");
            this.registerEventInjection(eventInjectionSpec);
        }
    }
}

