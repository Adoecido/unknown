package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class TileEntity
extends Wrapper {
    private static boolean usesLegacyCoordinates;


    public TileEntity(Object wrappedObject) {
        super(wrappedObject);
    }

    public BlockPos getBlockPos() {
        return new BlockPos(TileEntity.sonyVegasInstance.getMappingsMapperCompat().tileEntity.getBlockPos(this.I));
    }

    public int getX() {
        if (usesLegacyCoordinates) {
            return TileEntity.sonyVegasInstance.getMappingsMapperCompat().tileEntity.getX(this.I);
        }
        return this.getBlockPos().getX();
    }

    public static boolean setUsesLegacyCoordinates(boolean legacyCoordinates) {
        usesLegacyCoordinates = legacyCoordinates;
        return usesLegacyCoordinates;
    }

    public int getY() {
        if (usesLegacyCoordinates) {
            return TileEntity.sonyVegasInstance.getMappingsMapperCompat().tileEntity.getY(this.I);
        }
        return this.getBlockPos().getY();
    }

    public int getZ() {
        if (usesLegacyCoordinates) {
            return TileEntity.sonyVegasInstance.getMappingsMapperCompat().tileEntity.getZ(this.I);
        }
        return this.getBlockPos().getZ();
    }
}

