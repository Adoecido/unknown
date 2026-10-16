package vegas.sony.mapping;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MCapeLayer;

/** Locates Badlion's vanilla-compatible LayerCape method for the final render hook. */
public final class BadlionCapeLayerMappingTask extends JavassistMappingTask {
    private final MCapeLayer mapping;

    public BadlionCapeLayerMappingTask() {
        super(MappedClasses.CAPE_LAYER);
        this.mapping = new MCapeLayer();
    }

    @Override
    public void transform() {
        JavassistMappingTask.p(CapeRenderHook.class);
        MappingMethod method = this.mapping.renderMethod;
        if (this.E == null || method == null || method.hasResolutionFailed()) {
            return;
        }
        CtBehavior behavior = this.F(method);
        if (behavior == null) {
            return;
        }
        try {
            behavior.insertBefore("{ " + CapeRenderHook.class.getName()
                    + "#capeLayerEnter((java.lang.Object)$1); }");
            behavior.insertAfter("{ " + CapeRenderHook.class.getName()
                    + "#capeLayerExit((java.lang.Object)$1); }");
        } catch (CannotCompileException exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Throwable error) {
            throw new IllegalStateException("Cape layer hook prepare failed", error);
        }
    }

    @Override
    public int commit() {
        try {
            int result = super.commit();
            return result;
        } catch (Throwable error) {
            throw new IllegalStateException("Cape layer hook commit failed", error);
        }
    }
}
