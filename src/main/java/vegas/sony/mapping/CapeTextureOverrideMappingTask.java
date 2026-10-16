package vegas.sony.mapping;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import vegas.sony.SonyVegas;

public final class CapeTextureOverrideMappingTask extends JavassistMappingTask {
    public CapeTextureOverrideMappingTask() {
        super(MappedClasses.zt);
    }

    @Override
    public void transform() {
        JavassistMappingTask.p(CapeTextureOverrideHook.class);
        MappingMethod method = SonyVegas.INSTANCE.getMappings().Dk.getLocationCapeMethod();
        if (method == null || method.hasResolutionFailed()) {
            return;
        }
        CtBehavior behavior = this.F(method);
        if (behavior == null) {
            return;
        }
        try {
            behavior.insertAfter("{ $_ = ($r) " + CapeTextureOverrideHook.class.getName()
                    + "#override((java.lang.Object)$0, (java.lang.Object)$_); }");
        } catch (CannotCompileException exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Throwable error) {
            throw new IllegalStateException("Texture hook prepare failed", error);
        }
    }

    @Override
    public int commit() {
        try {
            int result = super.commit();
            return result;
        } catch (Throwable error) {
            throw new IllegalStateException("Texture hook commit failed", error);
        }
    }
}
