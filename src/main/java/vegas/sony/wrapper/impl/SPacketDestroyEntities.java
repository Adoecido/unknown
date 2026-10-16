package vegas.sony.wrapper.impl;

public class SPacketDestroyEntities
extends SPacketDestroyEntitiesBase {
    public int[] getEntityIds() {
        return SPacketDestroyEntities.sonyVegasInstance.getMappingsMapperCompat().C7.getEntityIds(this.I);
    }

    public SPacketDestroyEntities(Object handle) {
        super(handle);
    }
}
