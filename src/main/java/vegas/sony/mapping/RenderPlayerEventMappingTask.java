package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventPreRenderPlayerSpec;
import vegas.sony.event.impl.EventRenderPlayerPost;
import vegas.sony.event.impl.EventRenderPlayerPre;
import vegas.sony.event.impl.EventSetArmorModel;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class RenderPlayerEventMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().renderPlayer.renderMethod;
        if (!ForgeVersion.MC_1_21_0.d()) {
            if (ForgeVersion.MC_1_16_5.d()) {
                this.c(mappingMethod, EventRenderPlayerPre.class, "$0, $1, $3, $4");
                this.k(mappingMethod, EventRenderPlayerPost.class, "$0, $1, $3, $4");
            } else {
                this.c(mappingMethod, EventRenderPlayerPre.class, "$0, $1, $2, $3, $4, $6");
                this.k(mappingMethod, EventRenderPlayerPost.class, "$0, $1, $6");
                if (ForgeVersion.MC_1_7_10.L()) {
                    MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().renderPlayer.shouldRenderPassMethod;
                    EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod2, EventSetArmorModel.class);
                    eventInjectionSpec.setConstructorArguments("$1, $2, $3");
                    eventInjectionSpec.setReturnExpression("($r) $event.getResult()");
                    this.registerEventInjection(eventInjectionSpec);
                    MappingMethod mappingMethod3 = SonyVegas.INSTANCE.getMappings().renderPlayer.renderEquippedItemsMethod;
                    this.c(mappingMethod3, EventPreRenderPlayerSpec.class, "$1, $2");
                }
            }
        }
    }

    public RenderPlayerEventMappingTask() {
        super(MappedClasses.D0);
    }

}

