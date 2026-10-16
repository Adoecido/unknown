package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventMotion;
import vegas.sony.event.impl.EventPostLocalPlayerTick;
import vegas.sony.event.impl.EventPostMotion;
import vegas.sony.event.impl.EventPreLocalPlayerTick;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.event.impl.EventSetSprinting;
import vegas.sony.input.ChatCommandHandler;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.ForgeVersion;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

public class EntityPlayerSPEventMappingTask
extends JavassistMappingTask {
    private String W;
    private String y;
    private String N;
    private MappingMethod j = null;
    private String f;
    private String Z;
    private String L;

    private Object lambda$create$0() throws CannotCompileException, NotFoundException {
        this.m();
        return null;
    }

    public static String L(EntityPlayerSPEventMappingTask entityPlayerSPEventMappingTask) {
        return entityPlayerSPEventMappingTask.N;
    }

    private void Q$src$V$gyu8sk() {
        if (ForgeVersion.MC_1_21_10.d()) {
            return;
        }
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().CC.z;
        this.c(mappingMethod, EventPreLocalPlayerTick.class, "$0");
        this.k(mappingMethod, EventPostLocalPlayerTick.class, "$0");
    }

    public static String T(EntityPlayerSPEventMappingTask entityPlayerSPEventMappingTask) {
        return entityPlayerSPEventMappingTask.y;
    }

    public EntityPlayerSPEventMappingTask() {
        super(MappedClasses.z5);
    }

    public static String S(EntityPlayerSPEventMappingTask entityPlayerSPEventMappingTask) {
        return entityPlayerSPEventMappingTask.f;
    }

    private void p() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Rr.jW;
        this.c(mappingMethod, EventSetSprinting.class, "$0, $1");
    }

    private static Exception a(Exception exception) {
        return exception;
    }

    @Override
    public void transform() {
        this.installHook("setSprinting", this::p);
        this.installHook("localPlayerTick", this::Q$src$V$gyu8sk);
        if (ForgeVersion.MC_1_7_10.Y()) {
            this.installHook("chatCommands", this::installChatCommandHook);
            this.installHook("motion", () -> this.J(this::lambda$create$0));
        }
    }

    private void installChatCommandHook() {
        MappingMethod method = SonyVegas.INSTANCE.getMappings().CC.G;
        if (method == null || method.hasResolutionFailed()) {
            throw new IllegalStateException("sendChatMessage mapping is unavailable");
        }
        CtBehavior behavior = this.F(method);
        if (behavior == null) {
            throw new IllegalStateException("sendChatMessage bytecode is unavailable");
        }
        JavassistMappingTask.p(ChatCommandHandler.class);
        try {
            behavior.insertBefore("{ if (" + ChatCommandHandler.class.getName()
                    + "#process((java.lang.String)$1)) return; }");
        } catch (CannotCompileException failure) {
            throw new IllegalStateException("Unable to install chat command hook", failure);
        }
    }

    private void installHook(String name, Runnable hook) {
        try {
            hook.run();
        }
        catch (Throwable failure) {
            SonyVegas.logThrowable(failure);
            if (NativeBridge.isCheatBreaker189Runtime()) {
                CheatBreakerDebugLog.throwable("EntityPlayerSP hook " + name, failure);
            }
        }
    }

    public static String O(EntityPlayerSPEventMappingTask entityPlayerSPEventMappingTask) {
        return entityPlayerSPEventMappingTask.W;
    }

    public static String B(EntityPlayerSPEventMappingTask entityPlayerSPEventMappingTask) {
        return entityPlayerSPEventMappingTask.Z;
    }

    private void m() throws CannotCompileException, NotFoundException {
        String string;
        this.j = SonyVegas.INSTANCE.getMappings().CC.k;
        if (ForgeVersion.MC_1_20_6.d()) {
            this.L = SonyVegas.INSTANCE.getMappings().Rr.Q.getResolvedName();
            this.Z = SonyVegas.INSTANCE.getMappings().Rr.q.getResolvedName();
            this.y = SonyVegas.INSTANCE.getMappings().Rr.O.getResolvedName();
        } else {
            this.W = SonyVegas.INSTANCE.getMappings().Rr.jv.getResolvedName();
            this.f = SonyVegas.INSTANCE.getMappings().Rr.jT.getResolvedName();
            this.N = SonyVegas.INSTANCE.getMappings().Rr.U.getResolvedName();
        }
        String string2 = EventMotion.class.getName();
        if (this.j == null || this.j.hasResolutionFailed()) {
            throw new IllegalStateException("onUpdateWalkingPlayer mapping is unresolved");
        }
        CtBehavior ctBehavior = this.F(this.j);
        if (ctBehavior == null) {
            throw new IllegalStateException("onUpdateWalkingPlayer bytecode was not found: "
                    + this.j.getResolvedName() + this.j.getDescriptor());
        }
        CheatBreakerDebugLog.info("MOTION_HOOK target=" + this.j.getResolvedName()
                + " sig=" + this.j.getDescriptor() + " behavior=" + (ctBehavior == null ? "null" : ctBehavior.getLongName()));
        this.c(this.j, EventPreMotion.class, "$0");
        this.k(this.j, EventPostMotion.class, "$0");
        boolean bl = SonyVegas.INSTANCE.isForgeAbsent();
        if (bl && NativeBridge.gc(string = "aaa") == null) {
            bl = false;
        }
        boolean bl2 = bl;
        CtClass ctClass = this.i(MappedClasses.zc);
        EntityPlayerSPMotionExprEditor exprEditor = new EntityPlayerSPMotionExprEditor(this, ctClass, string2, bl2);
        ctBehavior.instrument(exprEditor);
        CheatBreakerDebugLog.info("MOTION_HOOK instrumented fieldNames=" + this.W + "/" + this.f + "/" + this.N
                + " declaringClassCheck=" + bl2 + " rewrites=" + exprEditor.getRewrittenFields());
    }

    public static String N(EntityPlayerSPEventMappingTask entityPlayerSPEventMappingTask) {
        return entityPlayerSPEventMappingTask.L;
    }
}
