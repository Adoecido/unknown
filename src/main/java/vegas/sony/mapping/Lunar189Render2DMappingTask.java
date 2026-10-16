package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventRender2D;
import javassist.CannotCompileException;
import javassist.CtBehavior;

/**
 * Lunar Client 1.8.9 Render2D hook.
 *
 * <p>Fires the shared {@link EventRender2D} at the TAIL of
 * GuiIngame.renderGameOverlay, which runs once per frame already inside the
 * client's 2D overlay matrix. EntityRenderer-anchored hooks rely on
 * call-pattern matching inside methods OptiFine rewrites on Lunar, so they
 * silently miss there; this anchor needs no pattern (method tail) and no
 * entity supertypes.
 *
 * <p>Lunar-only; every other runtime keeps its existing hook.
 */
public final class Lunar189Render2DMappingTask extends JavassistMappingTask {
    public Lunar189Render2DMappingTask() {
        super(MappedClasses.Zj);
    }

    @Override
    public void transform() {
        MappingMethod method = SonyVegas.INSTANCE.getMappings().titledScreen.renderHudMethod;
        if (method == null || method.hasResolutionFailed()) {
            throw new IllegalStateException("Lunar GuiIngame.renderGameOverlay unresolved");
        }
        CtBehavior behavior = this.F(method);
        if (behavior == null) {
            throw new IllegalStateException("Lunar GuiIngame.renderGameOverlay bytecode missing");
        }
        try {
            behavior.insertAfter("{" + EventRender2D.class.getName() + "#create();}");
        } catch (CannotCompileException exception) {
            throw new IllegalStateException("Lunar Render2D tail insert failed", exception);
        }
        vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_LUNAR render2d tail installed //$");
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Throwable error) {
            throw new IllegalStateException("Lunar Render2D prepare failed", error);
        }
    }

    @Override
    public int commit() {
        try {
            return super.commit();
        } catch (Throwable error) {
            throw new IllegalStateException("Lunar Render2D commit failed", error);
        }
    }
}
