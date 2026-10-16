package vegas.sony.module.blatant.backtrack;

import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.SPacketDestroyEntities;

public class BacktrackPacketReplayState
extends Packet {
    public BacktrackPacketReplayState(Object handle) {
        super(handle);
    }

    public int[] getDestroyedEntityIds() {
        if (ForgeVersion.MC_1_21_4.d()) {
            SPacketDestroyEntities destroyPacket = new SPacketDestroyEntities(BacktrackPacketReplayState.sonyVegasInstance.getMappingsMapperCompat().Rt.getEntityIdsContainer(this.I));
            return destroyPacket.getEntityIds();
        }
        return BacktrackPacketReplayState.sonyVegasInstance.getMappingsMapperCompat().Rt.getEntityIds(this.I);
    }
}
