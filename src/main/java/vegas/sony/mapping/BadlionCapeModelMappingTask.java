package vegas.sony.mapping;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MBadlionCapeModel;

/** Replaces Badlion's broken HD-atlas ModelRenderer cape call at its source. */
public final class BadlionCapeModelMappingTask extends JavassistMappingTask {
    private final MBadlionCapeModel mapping;

    public BadlionCapeModelMappingTask() {
        super(MappedClasses.ud);
        this.mapping = new MBadlionCapeModel();
    }

    @Override
    public void transform() {
        JavassistMappingTask.p(CapeRenderHook.class);
        MappingMethod method = this.mapping.renderCape;
        if (this.E == null || method == null || method.hasResolutionFailed()) {
            return;
        }
        CtBehavior behavior = this.F(method);
        if (behavior == null) {
            return;
        }
        try {
            behavior.insertBefore("{ if (" + CapeRenderHook.class.getName()
                    + "#renderCustomCape($1)) { return; } }");
        } catch (CannotCompileException exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Throwable error) {
            throw new IllegalStateException("Cape model hook prepare failed", error);
        }
    }

    @Override
    public int commit() {
        try {
            int result = super.commit();
            return result;
        } catch (Throwable error) {
            throw new IllegalStateException("Cape model hook commit failed", error);
        }
    }
}
