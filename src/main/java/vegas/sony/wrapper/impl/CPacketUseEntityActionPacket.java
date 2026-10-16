package vegas.sony.wrapper.impl;

public class CPacketUseEntityActionPacket
extends CPacketUseEntityAction {
    public CPacketUseEntityActionPacket(Object wrappedObject) {
        super(wrappedObject, null);
    }

    public Vec3 getLocation() {
        return new Vec3(CPacketUseEntityActionPacket.sonyVegasInstance.getMappingsMapperCompat().h6.getLocation(this.I));
    }
}
