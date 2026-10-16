package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ForgeEvent
extends Wrapper {
    public void setCancelled(boolean bl) {
        ForgeEvent.sonyVegasInstance.getMappingsMapperCompat().forgeEvent.setCanceled(this.getObject(), bl);
    }

    public ForgeEvent(Object object) {
        super(object);
    }
}
