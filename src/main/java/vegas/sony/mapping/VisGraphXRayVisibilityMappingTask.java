package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventVisGraphComputeVisibility;
import vegas.sony.event.impl.EventVisGraphSetOpaqueCube;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class VisGraphXRayVisibilityMappingTask
extends JavassistMappingTask {
    private static final String VISIBILITY_RETURN_EXPRESSION = "($r) $event.getVisibility()";

    @Override
    public void transform() {
        MappingMethod computeVisibilityMethod = SonyVegas.INSTANCE.getMappings().visGraph.computeVisibilityMethod;
        EventInjectionSpec computeVisibilityInjection = new EventInjectionSpec(computeVisibilityMethod, EventVisGraphComputeVisibility.class);
        computeVisibilityInjection.setReturnExpression(VISIBILITY_RETURN_EXPRESSION);
        this.registerEventInjection(computeVisibilityInjection);
        MappingMethod setOpaqueCubeMethod = SonyVegas.INSTANCE.getMappings().visGraph.setOpaqueCubeMethod;
        EventInjectionSpec setOpaqueCubeInjection = new EventInjectionSpec(setOpaqueCubeMethod, EventVisGraphSetOpaqueCube.class);
        setOpaqueCubeInjection.setInsertBefore(true);
        this.registerEventInjection(setOpaqueCubeInjection);
    }

    public VisGraphXRayVisibilityMappingTask() {
        super(MappedClasses.VIS_GRAPH);
    }
}
