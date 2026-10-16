package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

import java.util.function.Predicate;

public class EntityRayTraceBridge
extends Wrapper {
    public EntityRayTraceBridge(Object bridgeHandle) {
        super(bridgeHandle);
    }

    public RayTraceResult getClosestHit(Entity entity, float distance, Predicate<Object> predicate) {
        Object resultHandle = EntityRayTraceBridge.sonyVegasInstance.getMappingsMapperCompat().entityRayTraceBridge
                .getClosestHit(this.I, entity.getObject(), distance, predicate);
        return new RayTraceResult(resultHandle);
    }
}
