package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class CPacketUseEntityAction
extends Wrapper {
    private CPacketUseEntityAction(Object handle) {
        super(handle);
    }

    public CPacketUseEntityAction(Object handle, CPacketUseEntityActionConstructorMarker constructorMarker) {
        this(handle);
    }

    public CPacketUseEntity getType() {
        return new CPacketUseEntity(CPacketUseEntityAction.sonyVegasInstance.getMappingsMapperCompat().qz.getType(this.I), null);
    }
}
