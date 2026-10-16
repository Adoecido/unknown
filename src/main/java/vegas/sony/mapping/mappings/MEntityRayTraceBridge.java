package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MItemAttributeModifiersComponent;
import vegas.sony.ui.click.component.GuiComponent;
import java.util.function.Predicate;

public class MEntityRayTraceBridge
extends Mapping {
    private static final String GET_CLOSEST_HIT = "getClosesetHit";
    private final MappingMethod getClosestHitMethod;

    public Object getClosestHit(Object bridgeHandle, Object entityHandle, float distance, Predicate<Object> predicate) {
        return this.getClosestHitMethod.invokeObject(bridgeHandle, entityHandle, distance, predicate);
    }

    public MEntityRayTraceBridge() {
        this(MItemAttributeModifiersComponent.getItemAttributeModifiersControlFlowState());
    }

    private MEntityRayTraceBridge(GuiComponent[] controlFlowState) {
        super(MappedClasses.Dr);
        if (controlFlowState != null) {
            this.getClosestHitMethod = this.Y(
                    GET_CLOSEST_HIT, true, MappedClasses.DT, MappedClasses.zc, Float.TYPE, Predicate.class);
            return;
        }
        this.getClosestHitMethod = this.Y(
                GET_CLOSEST_HIT, true, MappedClasses.DT, MappedClasses.zc, Float.TYPE, Predicate.class);
        GuiComponent.setLegacyComponentState(new GuiComponent[5]);
    }
}
