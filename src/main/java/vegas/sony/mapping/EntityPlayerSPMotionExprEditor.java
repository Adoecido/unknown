package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.EntityPlayerSPEventMappingTask;
import vegas.sony.wrapper.impl.ForgeVersion;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public class EntityPlayerSPMotionExprEditor
extends ExprEditor {
    final String B;
    final boolean c;
    final EntityPlayerSPEventMappingTask p;
    final CtClass f;
    private int rewrittenFields;
    private boolean firstFieldLogged;

    @Override
    public void edit(MethodCall methodCall) throws CannotCompileException {
        if (ForgeVersion.MC_1_20_6.v()) {
            return;
        }
        try {
            if (methodCall.getMethodName().equals(EntityPlayerSPEventMappingTask.N(this.p)) && methodCall.getSignature().equals("()F") && methodCall.getMethod().getDeclaringClass().equals(this.f)) {
                String string = "$_ = " + this.B + "#getRotationYaw();";
                methodCall.replace(string);
                return;
            }
            if (methodCall.getMethodName().equals(EntityPlayerSPEventMappingTask.B(this.p)) && methodCall.getSignature().equals("()F") && methodCall.getMethod().getDeclaringClass().equals(this.f)) {
                String string = "$_ = " + this.B + "#getRotationPitch();";
                methodCall.replace(string);
                return;
            }
            if (methodCall.getMethodName().equals(EntityPlayerSPEventMappingTask.T(this.p)) && methodCall.getSignature().equals("()Z") && methodCall.getMethod().getDeclaringClass().equals(this.f)) {
                String string = "$_ = " + this.B + "#isOnGround();";
                methodCall.replace(string);
                return;
            }
        }
        catch (Exception exception) {
            vegas.sony.runtime.CheatBreakerDebugLog.throwable("MOTION_EXPR edit(MethodCall)", exception);
            SonyVegas.logThrowable(exception);
        }
    }

    public EntityPlayerSPMotionExprEditor(EntityPlayerSPEventMappingTask entityPlayerSPEventMappingTask, CtClass ctClass, String string, boolean bl) {
        this.p = entityPlayerSPEventMappingTask;
        this.f = ctClass;
        this.B = string;
        this.c = bl;
    }

    @Override
    public void edit(FieldAccess fieldAccess) throws CannotCompileException {
        try {
            if (ForgeVersion.MC_1_20_6.d()) {
                return;
            }
            if (fieldAccess.getFieldName().equals(EntityPlayerSPEventMappingTask.O(this.p)) && fieldAccess.getSignature().equals("F") && (!this.c || fieldAccess.getField().getDeclaringClass().equals(this.f))) {
                vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR match yaw at line " + fieldAccess.getLineNumber());
                String string = "$_ = " + this.B + "#getRotationYaw();";
                fieldAccess.replace(string);
                vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR rewrite yaw at line " + fieldAccess.getLineNumber());
                ++this.rewrittenFields;
                return;
            }
            if (fieldAccess.getFieldName().equals(EntityPlayerSPEventMappingTask.S(this.p)) && fieldAccess.getSignature().equals("F") && (!this.c || fieldAccess.getField().getDeclaringClass().equals(this.f))) {
                vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR match pitch at line " + fieldAccess.getLineNumber());
                String string = "$_ = " + this.B + "#getRotationPitch();";
                fieldAccess.replace(string);
                vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR rewrite pitch at line " + fieldAccess.getLineNumber());
                ++this.rewrittenFields;
                return;
            }
            if (fieldAccess.getFieldName().equals(EntityPlayerSPEventMappingTask.L(this.p)) && fieldAccess.getSignature().equals("Z") && (!this.c || fieldAccess.getField().getDeclaringClass().equals(this.f))) {
                vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR match onGround at line " + fieldAccess.getLineNumber());
                String string = "$_ = " + this.B + "#isOnGround();";
                fieldAccess.replace(string);
                vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR rewrite onGround at line " + fieldAccess.getLineNumber());
                ++this.rewrittenFields;
                return;
            }
        }
        catch (Exception exception) {
            vegas.sony.runtime.CheatBreakerDebugLog.throwable("MOTION_EXPR edit(FieldAccess) field=" + fieldAccess.getFieldName(), exception);
            SonyVegas.logThrowable(exception);
        }
        if (vegas.sony.runtime.NativeBridge.isCheatBreaker189Runtime()) {
            String sig = fieldAccess.getSignature();
            if ("F".equals(sig) || "Z".equals(sig)) {
                vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR unmatched field="
                        + fieldAccess.getFieldName() + " sig=" + sig
                        + " line=" + fieldAccess.getLineNumber());
            }
        } else if (!this.firstFieldLogged) {
            this.firstFieldLogged = true;
            vegas.sony.runtime.CheatBreakerDebugLog.info("MOTION_EXPR first field " + fieldAccess.getFieldName()
                    + " sig=" + fieldAccess.getSignature() + " line=" + fieldAccess.getLineNumber()
                    + " declaredCheck=" + this.c);
        }
    }

    public int getRewrittenFields() {
        return this.rewrittenFields;
    }

    private static Exception a(Exception exception) {
        return exception;
    }
}
