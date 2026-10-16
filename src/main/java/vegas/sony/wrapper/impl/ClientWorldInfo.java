package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ClientWorldInfo
extends Wrapper {
    public void setDayTime(long dayTime) {
        ClientWorldInfo.sonyVegasInstance.getMappingsMapperCompat().clientWorldInfo.setDayTime(this.I, dayTime);
    }

    public ClientWorldInfo(Object wrappedObject) {
        super(wrappedObject);
    }

    public void setGameTime(long gameTime) {
        ClientWorldInfo.sonyVegasInstance.getMappingsMapperCompat().clientWorldInfo.setGameTime(this.I, gameTime);
    }
}
