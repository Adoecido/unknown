package vegas.sony.mapping;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import vegas.sony.SonyVegas;

public class EntityBoundingBoxFilterMappingTask extends JavassistMappingTask {
    public EntityBoundingBoxFilterMappingTask() {
        super(MappedClasses.zc);
    }

    @Override
    public void transform() {
        JavassistMappingTask.p(EntityBoundingBoxFilterHook.class);
        CtBehavior behavior = this.F(SonyVegas.INSTANCE.getMappings().Rr.jQ);
        if (behavior == null) {
            return;
        }
        try {
            behavior.insertAfter("{ $_ = ($r) " + EntityBoundingBoxFilterHook.class.getName() + "#filter($0, $_); }");
        } catch (CannotCompileException exception) {
            SonyVegas.logThrowable(exception);
        }
    }
}
