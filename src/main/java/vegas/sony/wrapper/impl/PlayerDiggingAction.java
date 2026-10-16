package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class PlayerDiggingAction
extends Wrapper {
    public static PlayerDiggingAction releaseUseItem() {
        return new PlayerDiggingAction(PlayerDiggingAction.sonyVegasInstance.getMappingsMapperCompat().Ci.getReleaseUseItem());
    }

    public PlayerDiggingAction(Object wrappedObject) {
        super(wrappedObject);
    }
}
