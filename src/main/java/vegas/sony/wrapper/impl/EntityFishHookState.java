package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEntityFishHook;
import vegas.sony.wrapper.Wrapper;

public class EntityFishHookState
extends Wrapper {
    public boolean isOpen() {
        return MEntityFishHook.isOpen(EntityFishHookState.sonyVegasInstance.getMappingsMapperCompat().nettyChannel, this.I);
    }

    public CaughtEntity eventLoop() {
        return new CaughtEntity(MEntityFishHook.eventLoop(EntityFishHookState.sonyVegasInstance.getMappingsMapperCompat().nettyChannel, this.I));
    }

    public EntityFishHookState(Object object) {
        super(object);
    }
}
