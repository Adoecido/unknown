package vegas.sony.mapping;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import vegas.sony.SonyVegas;

/** Forces the local player's CAPE model part on while CapeManager is enabled. */
public final class CapeVisibilityOverrideMappingTask extends JavassistMappingTask {
    public CapeVisibilityOverrideMappingTask() {
        super(MappedClasses.Yl);
    }

    @Override
    public void transform() {
        JavassistMappingTask.p(CapeTextureOverrideHook.class);
        MappingMethod method = SonyVegas.INSTANCE.getMappings().Dk.getIsWearingModelPartMethod();
        if (method == null || method.hasResolutionFailed()) {
            return;
        }
        CtBehavior behavior = this.F(method);
        if (behavior == null) {
            return;
        }
        try {
            behavior.insertAfter("{ $_ = " + CapeTextureOverrideHook.class.getName()
                    + "#forceCapeVisible((java.lang.Object)$0, (java.lang.Object)$1, $_); }");
        } catch (CannotCompileException exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Throwable error) {
            throw new IllegalStateException("Visibility hook prepare failed", error);
        }
    }

    @Override
    public int commit() {
        try {
            int result = super.commit();
            return result;
        } catch (Throwable error) {
            throw new IllegalStateException("Visibility hook commit failed", error);
        }
    }
}
