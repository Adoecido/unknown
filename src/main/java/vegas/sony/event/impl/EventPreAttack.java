package vegas.sony.event.impl;

import vegas.sony.event.impl.EventAttackBase;

public class EventPreAttack
extends EventAttackBase {
    @Override
    public boolean fire() {
        return super.fire();
    }

    public EventPreAttack(Object targetHandle) {
        super(targetHandle);
    }
}
