package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.RenderBatchFlushCallbackMarker;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ThreadBoundCallbackMappingTask
extends JavassistMappingTask {
    private static final String c = "#call();}";

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().glCommandEncoder.drawFromBuffersMethod;
        CtBehavior ctBehavior = this.F(mappingMethod);
        try {
            ctBehavior.insertBefore("{" + RenderBatchFlushCallbackMarker.class.getName() + c);
        }
        catch (CannotCompileException cannotCompileException) {
            SonyVegas.logThrowable(cannotCompileException);
        }
    }

    public ThreadBoundCallbackMappingTask() {
        super(MappedClasses.zg);
    }
}
