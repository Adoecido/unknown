package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.InsertedCallbackLockMarker;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class GlStateManagerBlendFuncMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Dt.O;
        CtBehavior ctBehavior = this.F(mappingMethod);
        try {
            String string = "{if(" + InsertedCallbackLockMarker.class.getName() + "#check($1, $2)) {   return;}}";
            ctBehavior.insertBefore(string);
        }
        catch (CannotCompileException cannotCompileException) {
            SonyVegas.logThrowable(cannotCompileException);
        }
    }

    public GlStateManagerBlendFuncMappingTask() {
        super(MappedClasses.K);
    }
}
