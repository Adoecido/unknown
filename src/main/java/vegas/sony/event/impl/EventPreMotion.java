package vegas.sony.event.impl;

import vegas.sony.event.impl.EventMotion;
import vegas.sony.wrapper.impl.Entity;

public class EventPreMotion
extends EventMotion {
    @Override
    public boolean fire() {
        if (player.isNull()) {
            return false;
        }
        boolean fired = super.fire();
        EventMotion.storeRidingEntity(player.S$src$Lvegas_sony_wrapper_impl_Entity_$dgzs12());
        if (EventPreMotion.shouldAlwaysSend()) {
            player.T(new Entity(null));
        }
        return fired;
    }


    public EventPreMotion(Object playerHandle) {
        super(new Entity(playerHandle));
        alwaysSend = false;
        EventMotion.storeRotationYaw(EventMotion.player.J());
        EventMotion.storeRotationPitch(EventMotion.player.V());
        EventMotion.storeOnGround(EventMotion.player.b$src$Z$fqlxe4());
    }
}

