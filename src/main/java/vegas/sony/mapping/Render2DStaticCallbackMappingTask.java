package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.EventRender2DStaticCallback;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class Render2DStaticCallbackMappingTask
extends JavassistMappingTask {
    private static final String c = "#call();}";

    private static CannotCompileException a(CannotCompileException cannotCompileException) {
        return cannotCompileException;
    }

    @Override
    public void transform() {
        if (ForgeVersion.MC_26_1.d()) {
            return;
        }
        Render2DStaticCallbackMappingTask.p(EventRender2DStaticCallback.class);
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().titledScreen.renderHudMethod;
        CtBehavior ctBehavior = this.F(mappingMethod);
        try {
            ctBehavior.insertAfter("{" + EventRender2DStaticCallback.class.getName() + c);
        }
        catch (CannotCompileException cannotCompileException) {
            SonyVegas.logThrowable(cannotCompileException);
        }
    }

    public Render2DStaticCallbackMappingTask() {
        super(MappedClasses.Zj);
    }
}
