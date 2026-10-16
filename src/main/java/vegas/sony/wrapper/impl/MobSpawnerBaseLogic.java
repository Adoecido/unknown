package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class MobSpawnerBaseLogic
extends Wrapper {
    public MobSpawnerBaseLogic(Object wrappedObject) {
        super(wrappedObject);
    }


    public String getEntityName() {
        if (ForgeVersion.MC_1_12_2.d()) {
            Entity entity = new Entity(MobSpawnerBaseLogic.sonyVegasInstance.getMappings().mobSpawnerBaseLogic.getCachedEntity(this.I, ForgeVersion.MC_1_17.d() ? Minecraft.theWorld().getObject() : null));
            if (entity.isNotNull()) {
                return entity.getName();
            }
            return "";
        }
        return MobSpawnerBaseLogic.sonyVegasInstance.getMappings().mobSpawnerBaseLogic.getEntityNameToSpawn(this.I);
    }
}

