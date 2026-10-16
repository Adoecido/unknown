package vegas.sony.wrapper.impl;

public class CPacketEntityActionAction
extends CPacketEntityAction {
    public static CPacketEntityActionAction startSneaking() {
        return new CPacketEntityActionAction(CPacketEntityActionAction.sonyVegasInstance.getMappingsMapperCompat().DQ.getStartSneakingAction());
    }

    public static CPacketEntityActionAction stopSneaking() {
        return new CPacketEntityActionAction(CPacketEntityActionAction.sonyVegasInstance.getMappingsMapperCompat().DQ.getStopSneakingAction());
    }

    public CPacketEntityActionAction(Object handle) {
        super(handle);
    }
}
