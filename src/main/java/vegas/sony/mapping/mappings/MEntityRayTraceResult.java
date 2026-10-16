package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MEntityRayTraceResult
extends Mapping {
    private static final String CONSTRUCTOR = "<init>";
    private final MappingMethod getEntityMethod;
    private final MappingMethod constructor;

    public Object create(Object entityHandle, Object hitPositionHandle) {
        return this.constructor.newInstance(entityHandle, hitPositionHandle);
    }

    public MEntityRayTraceResult() {
        super(MappedClasses.zl);
        this.constructor = this.Y(CONSTRUCTOR, false, Void.TYPE, MappedClasses.zc, MappedClasses.qP);
        this.getEntityMethod = this.Y("getEntity", true, MappedClasses.zc);
    }

    public Object getEntity(Object rayTraceResultHandle) {
        return this.getEntityMethod.invokeObject(rayTraceResultHandle);
    }
}

