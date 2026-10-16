package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBlockRenderColorOverride;
import vegas.sony.event.impl.EventRenderPlayerPost;
import vegas.sony.event.impl.EventRenderPlayerPre;
import vegas.sony.mapping.expr.EventBlockRenderColorOverrideExprEditor;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.wrapper.impl.ForgeVersion;
import javassist.CtBehavior;

public class RenderLivingBaseEventMappingTask
extends JavassistMappingTask {
    private static Exception a(Exception exception) {
        return exception;
    }

    public RenderLivingBaseEventMappingTask() {
        super(MappedClasses.Fq);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod;
        if (ForgeVersion.MC_1_8_9.S().y()) {
            // Rotate draws the silent rotation on the local player model from
            // these two hooks. A failure here must not take the rest of the
            // task down with it, and it has to be visible in the log instead of
            // showing up as "Rotate does nothing".
            try {
                MappingMethod renderMethod = SonyVegas.INSTANCE.getMappings().CP.renderMethod;
                if (renderMethod == null || renderMethod.hasResolutionFailed()) {
                    CheatBreakerDebugLog.info("RLB doRender hook: method not resolved");
                } else {
                    this.c(renderMethod, EventRenderPlayerPre.class, "$0, $1, $2, $3, $4, $6");
                    JavassistMappingTask.p(EventRenderPlayerPost.class);
                    CtBehavior renderBehavior = this.F(renderMethod);
                    renderBehavior.insertAfter("{ new " + EventRenderPlayerPost.class.getName()
                            + "((java.lang.Object)$0, (java.lang.Object)$1, $6).fire(); }", true);
                    CheatBreakerDebugLog.info("RLB doRender hook installed name="
                            + renderMethod.getResolvedName());
                }
            } catch (Throwable throwable) {
                CheatBreakerDebugLog.throwable("RLB doRender hook", throwable);
                SonyVegas.logThrowable(throwable);
            }
        }
        if (ForgeVersion.MC_1_8_9.d()) {
            mappingMethod = SonyVegas.INSTANCE.getMappings().CP.O;
            String string = EventBlockRenderColorOverride.class.getName();
            CtBehavior ctBehavior = this.F(mappingMethod);
            this.O(mappingMethod, EventBlockRenderColorOverride.class, "", "false");
            try {
                ctBehavior.instrument(new EventBlockRenderColorOverrideExprEditor(this, string));
            }
            catch (Exception exception) {
                SonyVegas.logThrowable(exception);
            }
        }
        if (ForgeVersion.MC_1_7_10.L() && SonyVegas.INSTANCE.isForgeAbsent()) {
            mappingMethod = SonyVegas.INSTANCE.getMappings().CP.r;
            this.c(mappingMethod, EventPreRenderEntityForgeCallback.class, "$1, $2, $3, $4");
        }
    }
}
