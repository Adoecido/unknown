package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.event.impl.EventMouseOverUpdate;
import vegas.sony.event.impl.EventPostTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MinecraftTickCallbackExprEditor;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.ForgeVersion;
import javassist.CtBehavior;
import javassist.expr.MethodCall;

public class MinecraftTickEventMappingTask
extends JavassistMappingTask {
    private static Exception a(Exception exception) {
        return exception;
    }

    public MinecraftTickEventMappingTask() {
        super(MappedClasses.uP);
    }

    @Override
    public void transform() {
        try {
            MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().U.a;
            MappingMethod loopMethod = SonyVegas.INSTANCE.getMappings().U.v;
            if (NativeBridge.isBadlion189Runtime()) {
                // HOOK_ prefix: CheatBreakerDebugLog.info drops other prefixes.
                CheatBreakerDebugLog.info("HOOK_TICK target=" + this.E
                        + " loader=" + (this.E == null ? "null" : String.valueOf(this.E.getClassLoader()))
                        + " runTick=" + describe(mappingMethod)
                        + " runGameLoop=" + describe(loopMethod));
            }
            CtBehavior ctBehavior = this.F(loopMethod);
            boolean[] blArray = new boolean[]{false};
            if (ctBehavior == null && !NativeBridge.isBadlion189Runtime()) {
                // Preserve the original fail-fast outside Badlion bring-up.
                ctBehavior.instrument(new MinecraftTickCallbackExprEditor(this, mappingMethod, blArray));
            } else if (ctBehavior == null) {
                CheatBreakerDebugLog.info("HOOK_TICK runGameLoop bytecode missing, fallback to runTick ends //$");
            } else {
                ctBehavior.instrument(new MinecraftTickCallbackExprEditor(this, mappingMethod, blArray));
            }
            if (NativeBridge.isBadlion189Runtime()) {
                CheatBreakerDebugLog.info("HOOK_TICK exprMatched=" + blArray[0] + " fallback=" + (!blArray[0]) + " //$");
            }
            if (!blArray[0]) {
                this.O(mappingMethod, EventPreTick.class, "", "");
                this.j(mappingMethod, EventPostTick.class, "", "");
            }
            if (ForgeVersion.MC_1_20_6.d()) {
                MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().U.q;
                this.O(mappingMethod2, EventClickMouse.class, "", "false");
                if (ForgeVersion.MC_26_1.d() && SonyVegas.INSTANCE.getMappings().U.i != null && !SonyVegas.INSTANCE.getMappings().U.i.hasResolutionFailed()) {
                    this.k(SonyVegas.INSTANCE.getMappings().U.i, EventMouseOverUpdate.class, "$1");
                }
            }
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to create hooks", exception);
        }
    }

    public static boolean y(MethodCall methodCall, MappingMethod mappingMethod) {
        return MinecraftTickEventMappingTask.c(methodCall, mappingMethod);
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
