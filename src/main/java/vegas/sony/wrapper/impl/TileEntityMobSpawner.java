package vegas.sony.wrapper.impl;

public class TileEntityMobSpawner
extends TileEntity {
    public MobSpawnerBaseLogic getSpawnerBaseLogic() {
        return new MobSpawnerBaseLogic(TileEntityMobSpawner.sonyVegasInstance.getMappingsMapperCompat().mobSpawnerTileEntity.getSpawnerBaseLogic(this.I));
    }

    public TileEntityMobSpawner(Object wrappedObject) {
        super(wrappedObject);
    }
}
