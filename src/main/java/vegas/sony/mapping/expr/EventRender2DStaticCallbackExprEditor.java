package vegas.sony.mapping.expr;

import vegas.sony.mapping.EntityRendererEventMappingTask;
import vegas.sony.mapping.EventRender2DStaticCallback;
import vegas.sony.mapping.MappingMethod;
import java.util.concurrent.atomic.AtomicBoolean;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class EventRender2DStaticCallbackExprEditor
extends ExprEditor {
    final EntityRendererEventMappingTask P;
    final AtomicBoolean u;
    final MappingMethod t;

    private static CannotCompileException a(CannotCompileException cannotCompileException) {
        return cannotCompileException;
    }

    public EventRender2DStaticCallbackExprEditor(EntityRendererEventMappingTask entityRendererEventMappingTask, AtomicBoolean atomicBoolean, MappingMethod mappingMethod) {
        this.P = entityRendererEventMappingTask;
        this.u = atomicBoolean;
        this.t = mappingMethod;
    }

    @Override
    public void edit(MethodCall methodCall) throws CannotCompileException {
        if (this.u.get()) {
            return;
        }
        if (methodCall.getMethodName().equals(this.t.getResolvedName()) && methodCall.getSignature().equals(this.t.getDescriptor())) {
            // CheatBreaker draws its own modules in this call. Dispatch our HUD
            // after it returns so TargetInfo and notifications stay in front.
            methodCall.replace("{ $_ = $proceed($$); " + EventRender2DStaticCallback.class.getName() + "#call(); }");
            this.u.set(true);
        }
    }
}
