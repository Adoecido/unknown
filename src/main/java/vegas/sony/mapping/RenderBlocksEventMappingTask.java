package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBlockFluidRender;
import vegas.sony.event.impl.EventBlockRenderBounds;
import vegas.sony.event.impl.EventLegacyXRayRenderFaceXNeg;
import vegas.sony.event.impl.EventLegacyXRayRenderFaceXPos;
import vegas.sony.event.impl.EventLegacyXRayRenderFaceYNeg;
import vegas.sony.event.impl.EventLegacyXRayRenderFaceYPos;
import vegas.sony.event.impl.EventLegacyXRayRenderFaceZNeg;
import vegas.sony.event.impl.EventLegacyXRayRenderFaceZPos;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class RenderBlocksEventMappingTask
extends JavassistMappingTask {
    public RenderBlocksEventMappingTask() {
        super(MappedClasses.q5);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderBlockByRenderTypeMethod;
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventBlockRenderBounds.class);
        eventInjectionSpec.setConstructorArguments("$0, $1");
        eventInjectionSpec.setReturnExpression("false");
        this.registerEventInjection(eventInjectionSpec);
        mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderStandardBlockMethod;
        eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventBlockFluidRender.class);
        eventInjectionSpec.setConstructorArguments("$0, $1, $2, $3, $4");
        eventInjectionSpec.setReturnExpression("$event.isResult()");
        this.registerEventInjection(eventInjectionSpec);
        mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderFaceXNegMethod;
        eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventLegacyXRayRenderFaceYNeg.class);
        eventInjectionSpec.setConstructorArguments("$1");
        this.registerEventInjection(eventInjectionSpec);
        mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderFaceXPosMethod;
        eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventLegacyXRayRenderFaceYPos.class);
        eventInjectionSpec.setConstructorArguments("$1");
        this.registerEventInjection(eventInjectionSpec);
        mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderFaceYNegMethod;
        eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventLegacyXRayRenderFaceZNeg.class);
        eventInjectionSpec.setConstructorArguments("$1");
        this.registerEventInjection(eventInjectionSpec);
        mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderFaceYPosMethod;
        eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventLegacyXRayRenderFaceZPos.class);
        eventInjectionSpec.setConstructorArguments("$1");
        this.registerEventInjection(eventInjectionSpec);
        mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderFaceZNegMethod;
        eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventLegacyXRayRenderFaceXNeg.class);
        eventInjectionSpec.setConstructorArguments("$1");
        this.registerEventInjection(eventInjectionSpec);
        mappingMethod = SonyVegas.INSTANCE.getMappings().renderBlocks.renderFaceZPosMethod;
        eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventLegacyXRayRenderFaceXPos.class);
        eventInjectionSpec.setConstructorArguments("$1");
        this.registerEventInjection(eventInjectionSpec);
    }
}
