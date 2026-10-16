package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventEntityRendererMouseUpdate;
import vegas.sony.event.impl.EventMouseOverUpdate;
import vegas.sony.event.impl.EventPostEntityRendererMouseUpdate;
import vegas.sony.event.impl.EventPostRenderHand;
import vegas.sony.event.impl.EventPostRenderTick;
import vegas.sony.event.impl.EventPreEntityRendererMouseUpdate;
import vegas.sony.event.impl.EventPreRenderHand;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.EventRender2DStaticCallback;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.expr.EventFogDensityInjectionExprEditor;
import vegas.sony.mapping.expr.EventRender2DInjectionExprEditor;
import vegas.sony.mapping.expr.EventRender2DStaticCallbackExprEditor;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.ForgeVersion;
import java.util.concurrent.atomic.AtomicBoolean;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.NotFoundException;

public class EntityRendererEventMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        if (ForgeVersion.MC_1_21_4.b().y()) {
            EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(SonyVegas.INSTANCE.getMappings().RY.J, EventPreRenderTick.class);
            eventInjectionSpec.setConstructorArguments("$1");
            eventInjectionSpec.setCancelableReturnGuard(false);
            eventInjectionSpec.setInsertBefore(true);
            this.registerEventInjection(eventInjectionSpec);
            this.i();
        }
        if (ForgeVersion.MC_1_16_5.v()) {
            this.c(SonyVegas.INSTANCE.getMappings().RY.B, EventPreRenderHand.class, "");
            this.k(SonyVegas.INSTANCE.getMappings().RY.B, EventPostRenderHand.class, "");
        }
        if (ForgeVersion.MC_1_21_11.d()) {
            this.c(SonyVegas.INSTANCE.getMappings().RY.C, EventPreEntityRendererMouseUpdate.class, "");
            this.k(SonyVegas.INSTANCE.getMappings().RY.C, EventEntityRendererMouseUpdate.class, "$1");
            this.k(SonyVegas.INSTANCE.getMappings().RY.a, EventPostEntityRendererMouseUpdate.class, "");
        } else {
            this.c(SonyVegas.INSTANCE.getMappings().RY.a, EventPreEntityRendererMouseUpdate.class, "");
            this.k(SonyVegas.INSTANCE.getMappings().RY.a, EventPostEntityRendererMouseUpdate.class, "");
        }
        if (ForgeVersion.MC_26_1.v() && SonyVegas.INSTANCE.getMappings().RY.k != null && !SonyVegas.INSTANCE.getMappings().RY.k.hasResolutionFailed()) {
            this.k(SonyVegas.INSTANCE.getMappings().RY.k, EventMouseOverUpdate.class, "$1");
        }
        this.J(this::lambda$create$0);
        if (ForgeVersion.MC_26_1.d()) {
            this.J(this::lambda$create$1);
        }
        this.J(this::lambda$create$2);
    }

    private void J$src$V$f5bloi() throws CannotCompileException {
        // Badlion draws its own overlay after the vanilla HUD. Its final frame
        // callback lives in Display.update(boolean), so do not also draw here.
        if (ForgeVersion.MC_1_20_6.d() || NativeBridge.isBadlion189Runtime()) {
            return;
        }
        String string = ForgeVersion.MC_1_17.d() ? "(IIF)V" : (ForgeVersion.MC_1_16_5.d() ? "()V" : "(IF)V");
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        MappingMethod renderTarget = SonyVegas.INSTANCE.getMappings().RY.J;
        if (renderTarget != null) {
            vegas.sony.runtime.CheatBreakerDebugLog.info("ERET f5bloi target name="
                    + renderTarget.getResolvedName()
                    + " sig=" + renderTarget.getDescriptor()
                    + " owner=" + renderTarget.getOwnerClass()
                    + " failed=" + renderTarget.hasResolutionFailed());
        } else {
            vegas.sony.runtime.CheatBreakerDebugLog.info("ERET f5bloi target=null");
            return;
        }
        CtBehavior ctBehavior = null;
        try {
            ctBehavior = this.F(renderTarget);
        }
        catch (Throwable t) {
            vegas.sony.runtime.CheatBreakerDebugLog.throwable("ERET F(RY.J)", t);
            return;
        }
        if (ctBehavior == null) {
            vegas.sony.runtime.CheatBreakerDebugLog.info("ERET f5bloi: target method not found in bytecode owner="
                    + renderTarget.getOwnerClass()
                    + " name=" + renderTarget.getResolvedName()
                    + " sig=" + renderTarget.getDescriptor());
            return;
        }
        try {
            ctBehavior.instrument(new EventRender2DInjectionExprEditor(this, string, atomicBoolean));
        }
        catch (Throwable t) {
            vegas.sony.runtime.CheatBreakerDebugLog.throwable("ERET instrument", t);
            return;
        }
        vegas.sony.runtime.CheatBreakerDebugLog.info("ERET f5bloi done expectedCallSig=" + string + " matched=" + atomicBoolean.get());
    }

    private Object lambda$create$0() throws CannotCompileException, NotFoundException {
        this.J$src$V$f5bloi();
        return null;
    }

    private Object lambda$create$2() throws CannotCompileException, NotFoundException {
        this.r();
        return null;
    }

    private void r() throws CannotCompileException {
        if (ForgeVersion.MC_1_16_5.v()) {
            CtBehavior ctBehavior = this.F(SonyVegas.INSTANCE.getMappings().RY.L);
            MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().hi.A;
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            ctBehavior.instrument(new EventFogDensityInjectionExprEditor(this, mappingMethod, atomicBoolean));
        }
    }

    private static Exception a(Exception exception) {
        return exception;
    }

    private void i() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().RY.J;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventPostRenderTick.class);
        eventInjectionSpec.setConstructorArguments("$1");
        eventInjectionSpec.setCancelableReturnGuard(false);
        eventInjectionSpec.setInsertBefore(false);
        this.registerEventInjection(eventInjectionSpec);
    }

    private void B$src$V$f0x8xm() throws CannotCompileException {
        EntityRendererEventMappingTask.p(EventRender2DStaticCallback.class);
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Ca.z;
        MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().RY.J;
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        CtBehavior ctBehavior = this.F(mappingMethod2);
        ctBehavior.instrument(new EventRender2DStaticCallbackExprEditor(this, atomicBoolean, mappingMethod));
    }

    private Object lambda$create$1() throws CannotCompileException, NotFoundException {
        this.B$src$V$f0x8xm();
        return null;
    }

    public EntityRendererEventMappingTask() {
        super(MappedClasses.FW);
    }
}
