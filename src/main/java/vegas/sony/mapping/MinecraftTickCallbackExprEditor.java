package vegas.sony.mapping;

import vegas.sony.mapping.EventPostTickCallback;
import vegas.sony.mapping.EventPreTickCallback;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MinecraftTickEventMappingTask;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class MinecraftTickCallbackExprEditor
extends ExprEditor {
    final MinecraftTickEventMappingTask a;
    final MappingMethod m;
    final boolean[] W;

    public MinecraftTickCallbackExprEditor(MinecraftTickEventMappingTask minecraftTickEventMappingTask, MappingMethod mappingMethod, boolean[] blArray) {
        this.a = minecraftTickEventMappingTask;
        this.m = mappingMethod;
        this.W = blArray;
    }

    @Override
    public void edit(MethodCall methodCall) throws CannotCompileException {
        if (MinecraftTickEventMappingTask.y(methodCall, this.m)) {
            this.W[0] = true;
            methodCall.replace("{" + EventPreTickCallback.class.getName() + "#call();$_ = $proceed($$);" + EventPostTickCallback.class.getName() + "#call();}");
        }
    }

    private static CannotCompileException a(CannotCompileException cannotCompileException) {
        return cannotCompileException;
    }
}

