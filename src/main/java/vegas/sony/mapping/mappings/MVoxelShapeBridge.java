package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MVoxelShapeBridge
extends Mapping {
    private final MappingMethod bufferSourceMethod;

    public MVoxelShapeBridge() {
        super(MappedClasses.ZL);
        this.bufferSourceMethod = this.Y("bufferSource", true, MappedClasses.lp, new Class[]{});
    }

    public Object getBufferSource(Object renderer) {
        return this.bufferSourceMethod.invokeObject(renderer, new Object[0]);
    }
}

