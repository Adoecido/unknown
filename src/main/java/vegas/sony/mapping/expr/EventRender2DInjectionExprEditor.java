package vegas.sony.mapping.expr;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.mapping.EntityRendererEventMappingTask;
import vegas.sony.wrapper.impl.ForgeVersion;
import java.util.concurrent.atomic.AtomicBoolean;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class EventRender2DInjectionExprEditor
extends ExprEditor {
    final EntityRendererEventMappingTask N;
    final AtomicBoolean g;
    final String s;

    public EventRender2DInjectionExprEditor(EntityRendererEventMappingTask entityRendererEventMappingTask, String string, AtomicBoolean atomicBoolean) {
        this.N = entityRendererEventMappingTask;
        this.s = string;
        this.g = atomicBoolean;
    }

    private static CannotCompileException a(CannotCompileException cannotCompileException) {
        return cannotCompileException;
    }

    @Override
    public void edit(MethodCall methodCall) throws CannotCompileException {
        if (!methodCall.getSignature().equals(this.s)) {
            return;
        }
        if (ForgeVersion.MC_1_7_10.L() && !methodCall.getMethodName().equals("glAlphaFunc")) {
            return;
        }
        if (ForgeVersion.MC_1_7_10.Y() && ForgeVersion.MC_1_16_5.v() && !methodCall.getMethodName().equals(SonyVegas.INSTANCE.getMappings().Dt.q.getResolvedName())) {
            return;
        }
        if (ForgeVersion.MC_1_16_5.d() && ForgeVersion.MC_1_17.v() && !methodCall.getMethodName().equals(SonyVegas.INSTANCE.getMappings().f.v.getResolvedName())) {
            return;
        }
        methodCall.replace("{ $_ = $proceed($$); " + EventRender2D.class.getName() + "#create(); }");
        this.g.set(true);
    }
}
