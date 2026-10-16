package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.asm.helper.DescUtils;
import vegas.sony.asm.helper.TypedIndexedLocal;
import vegas.sony.asm.transform.ClassTransformer;
import vegas.sony.event.impl.EventScoreboardObjectiveRender;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.ForgeVersion;

public class ScoreboardObjectiveRenderMappingTask
extends ClassTransformer {

    public ScoreboardObjectiveRenderMappingTask() {
        super(MappedClasses.Zj);
    }

    @Override
    public void transform() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return;
        }
        MappingMethod method = SonyVegas.INSTANCE.getMappings().titledScreen.renderScoreboardMethod;
        if (method == null) {
            vegas.sony.runtime.CheatBreakerDebugLog.info("SCOREBOARD_HOOK renderScoreboardMethod is null"
                    + " version=" + ForgeVersion.c());
            return;
        }
        if (method.hasResolutionFailed()) {
            vegas.sony.runtime.CheatBreakerDebugLog.info("SCOREBOARD_HOOK renderScoreboardMethod resolution failed"
                    + " original=" + method.getOriginalName()
                    + " version=" + ForgeVersion.c());
        }
        if (ForgeVersion.MC_1_7_10.L()) {
            this.injectEventAtEntry(method, EventScoreboardObjectiveRender.class, new TypedIndexedLocal(1, DescUtils.getDescriptor(MappedClasses.Y)).setDescriptorClass(Object.class), new TypedIndexedLocal(2, "I"), new TypedIndexedLocal(3, "I"), new TypedIndexedLocal(4, DescUtils.getDescriptor(MappedClasses.uQ)).setDescriptorClass(Object.class));
        } else if (ForgeVersion.MC_1_16_5.d()) {
            this.injectEventAtEntry(method, EventScoreboardObjectiveRender.class, new TypedIndexedLocal(1, DescUtils.getDescriptor(MappedClasses.DQ)).setDescriptorClass(Object.class), new TypedIndexedLocal(2, DescUtils.getDescriptor(MappedClasses.Y)).setDescriptorClass(Object.class));
        } else {
            this.injectEventAtEntry(method, EventScoreboardObjectiveRender.class, new TypedIndexedLocal(1, DescUtils.getDescriptor(MappedClasses.Y)).setDescriptorClass(Object.class), new TypedIndexedLocal(2, DescUtils.getDescriptor(MappedClasses.Zz)).setDescriptorClass(Object.class));
        }
    }
}

