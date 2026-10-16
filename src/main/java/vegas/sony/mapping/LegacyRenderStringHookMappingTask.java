package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.LegacyRenderStringEventRender3DCallback;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class LegacyRenderStringHookMappingTask
extends JavassistMappingTask {
    private static final String c = "#call($1);}";

    public LegacyRenderStringHookMappingTask() {
        super(MappedClasses.Vk);
    }

    @Override
    public void transform() {
        LegacyRenderStringHookMappingTask.p(LegacyRenderStringEventRender3DCallback.class);
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().C5.A;
        CtBehavior ctBehavior = this.F(mappingMethod);
        try {
            ctBehavior.insertBefore("{" + LegacyRenderStringEventRender3DCallback.class.getName() + c);
        }
        catch (CannotCompileException cannotCompileException) {
            SonyVegas.logThrowable(cannotCompileException);
        }
    }
}
