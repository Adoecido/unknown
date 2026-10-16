package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SessionType
extends Wrapper {
    public SessionType mojang() {
        return new SessionType(SessionType.sonyVegasInstance.getMappingsMapperCompat().sessionType.getMojang());
    }

    public SessionType msa() {
        return new SessionType(SessionType.sonyVegasInstance.getMappingsMapperCompat().sessionType.getMsa());
    }

    public SessionType legacy() {
        return new SessionType(SessionType.sonyVegasInstance.getMappingsMapperCompat().sessionType.getLegacy());
    }

    public SessionType(Object wrappedObject) {
        super(wrappedObject);
    }
}
