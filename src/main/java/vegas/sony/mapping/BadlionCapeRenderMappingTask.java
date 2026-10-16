package vegas.sony.mapping;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import vegas.sony.SonyVegas;

/** Locates the final Badlion 1.8.9 player renderer before replacing cape geometry. */
public final class BadlionCapeRenderMappingTask extends JavassistMappingTask {
    public BadlionCapeRenderMappingTask() {
        super(MappedClasses.D0);
    }

    @Override
    public void transform() {
        JavassistMappingTask.p(CapeRenderHook.class);
        MappingMethod method = SonyVegas.INSTANCE.getMappings().renderPlayer.renderMethod;
        if (method == null || method.hasResolutionFailed()) {
            return;
        }
        CtBehavior behavior = this.F(method);
        if (behavior == null) {
            return;
        }
        try {
            behavior.insertBefore("{ " + CapeRenderHook.class.getName()
                    + "#renderPlayerEnter((java.lang.Object)$1); }");
            behavior.insertAfter("{ " + CapeRenderHook.class.getName()
                    + "#renderPlayerExit((java.lang.Object)$1); }");
        } catch (CannotCompileException exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Throwable error) {
            throw new IllegalStateException("Player render hook prepare failed", error);
        }
    }

    @Override
    public int commit() {
        try {
            int result = super.commit();
            return result;
        } catch (Throwable error) {
            throw new IllegalStateException("Player render hook commit failed", error);
        }
    }
}
