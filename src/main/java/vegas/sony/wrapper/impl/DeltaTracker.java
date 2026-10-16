package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MDeltaTracker;
import vegas.sony.wrapper.Wrapper;

public class DeltaTracker
extends Wrapper {
    public float getGameTimeDeltaPartialTick(boolean runsNormally) {
        return MDeltaTracker.getGameTimeDeltaPartialTick(DeltaTracker.sonyVegasInstance.getMappingsMapperCompat().deltaTracker, this.I, runsNormally);
    }

    public DeltaTracker(Object handle) {
        super(handle);
    }

    public float getGameTimeDeltaTicks() {
        return MDeltaTracker.getGameTimeDeltaTicks(DeltaTracker.sonyVegasInstance.getMappingsMapperCompat().deltaTracker, this.I);
    }
}
