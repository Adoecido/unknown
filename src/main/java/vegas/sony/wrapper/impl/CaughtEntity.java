package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MCaughtEntityActionBridge;
import vegas.sony.wrapper.Wrapper;

public class CaughtEntity
extends Wrapper {
    public CaughtEntity(Object object) {
        super(object);
    }

    public boolean inEventLoop() {
        return MCaughtEntityActionBridge.inEventLoop(CaughtEntity.sonyVegasInstance.getMappingsMapperCompat().eventLoop, this.I);
    }

    public void execute(Runnable runnable) {
        MCaughtEntityActionBridge.execute(CaughtEntity.sonyVegasInstance.getMappingsMapperCompat().eventLoop, this.I, runnable);
    }
}
