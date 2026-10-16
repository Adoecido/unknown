package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPostPlayerTick;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.PlayerNameFormatCallback;
import vegas.sony.wrapper.impl.ForgeVersion;

public class PlayerTickEventMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Rr.V;
        if (vegas.sony.runtime.NativeBridge.isBadlion189Runtime()) {
            // HOOK_ prefix: CheatBreakerDebugLog.info drops other prefixes.
            vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_PTICK target=" + this.E
                    + " loader=" + (this.E == null ? "null" : String.valueOf(this.E.getClassLoader()))
                    + " method=" + describe(mappingMethod));
        }
        this.c(mappingMethod, EventPrePlayerTick.class, "$0");
        this.k(mappingMethod, EventPostPlayerTick.class, "$0");
        if (ForgeVersion.MC_1_21_10.v()) {
            MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().Rr.jO;
            EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod2, PlayerNameFormatCallback.class);
            eventInjectionSpec.setConstructorArguments("$0");
            eventInjectionSpec.setReturnExpression("($r) $event.getRawDisplayName()");
            this.registerEventInjection(eventInjectionSpec);
        }
    }

    public PlayerTickEventMappingTask() {
        super(MappedClasses.Yl);
    }

    private static String describe(MappingMethod method) {
        if (method == null) {
            return "null";
        }
        try {
            return method.getResolvedName() + method.getDescriptor()
                    + " failed=" + method.hasResolutionFailed();
        } catch (Throwable failure) {
            return "describe-failed:" + failure.getClass().getSimpleName();
        }
    }
}
