package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class CPacketUseEntity
extends Wrapper {
    private CPacketUseEntity(Object handle) {
        super(handle);
    }

    public static CPacketUseEntity attack() {
        return new CPacketUseEntity(CPacketUseEntity.sonyVegasInstance.getMappingsMapperCompat().Ra.getAttackAction());
    }

    public static CPacketUseEntity interactAt() {
        return new CPacketUseEntity(CPacketUseEntity.sonyVegasInstance.getMappingsMapperCompat().Ra.getInteractAtAction());
    }

    public static CPacketUseEntity interact() {
        return new CPacketUseEntity(CPacketUseEntity.sonyVegasInstance.getMappingsMapperCompat().Ra.getInteractAction());
    }

    public CPacketUseEntity(Object handle, CPacketUseEntityActionConstructorMarker constructorMarker) {
        this(handle);
    }
}
