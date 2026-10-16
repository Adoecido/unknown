package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class NetworkPacketEventMappingTask
extends JavassistMappingTask {
    public NetworkPacketEventMappingTask() {
        super(MappedClasses.FO);
    }

    @Override
    public void transform() {
        this.k();
    }


    private void k() {
        EventInjectionSpec eventInjectionSpec;
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Do.a;
        MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().Do.O;
        if (vegas.sony.runtime.NativeBridge.isBadlion189Runtime()) {
            // HOOK_ prefix: CheatBreakerDebugLog.info drops other prefixes.
            vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_PACKET target=" + this.E
                    + " loader=" + (this.E == null ? "null" : String.valueOf(this.E.getClassLoader()))
                    + " recv=" + describe(mappingMethod)
                    + " send=" + describe(mappingMethod2));
        }
        if (mappingMethod != null && !mappingMethod.hasResolutionFailed()) {
            eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventPacketReceive.class);
            eventInjectionSpec.setConstructorArguments("$0, $2");
            eventInjectionSpec.setAfterCode("$2 = (" + MappedClasses.Fm.getName() + ") $event.getPacketInstance();");
            this.registerEventInjection(eventInjectionSpec);
        }
        eventInjectionSpec = new EventInjectionSpec(mappingMethod2, EventPacketSend.class);
        eventInjectionSpec.setConstructorArguments("$0, $1");
        eventInjectionSpec.setAfterCode("$1 = (" + MappedClasses.Fm.getName() + ") $event.getPacketInstance();");
        this.registerEventInjection(eventInjectionSpec);
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

