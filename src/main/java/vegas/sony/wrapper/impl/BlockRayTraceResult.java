package vegas.sony.wrapper.impl;

public class BlockRayTraceResult
extends RayTraceResult {
    public BlockRayTraceResult(Object wrappedObject) {
        super(wrappedObject);
    }

    public static BlockRayTraceResult createMiss(Vec3 hitLocation, Direction direction, BlockPos blockPos) {
        return new BlockRayTraceResult(BlockRayTraceResult.sonyVegasInstance.getMappings().blockRayTraceResult.createMiss(hitLocation.getObject(), direction.getObject(), blockPos.getObject()));
    }

    public boolean isInside() {
        return BlockRayTraceResult.sonyVegasInstance.getMappings().blockRayTraceResult.isInside(this.I);
    }
}
