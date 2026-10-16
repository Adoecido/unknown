package vegas.sony.wrapper.impl;

public class TileEntityChest
extends TileEntity {
    public float getLidOpenness() {
        if (ForgeVersion.MC_1_17.d()) {
            return TileEntityChest.sonyVegasInstance.getMappingsMapperCompat().chestTileEntity.getOpenness(this.getObject(), 0.0f);
        }
        return TileEntityChest.sonyVegasInstance.getMappingsMapperCompat().chestTileEntity.getStoredOpenness(this.getObject());
    }

    public TileEntityChest(Object wrappedObject) {
        super(wrappedObject);
    }

    public int getOpenCount() {
        return TileEntityChest.sonyVegasInstance.getMappingsMapperCompat().chestTileEntity.getOpenCount(this.getObject());
    }

}

