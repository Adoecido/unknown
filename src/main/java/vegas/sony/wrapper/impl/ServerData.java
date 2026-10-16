package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MServerData;
import vegas.sony.wrapper.Wrapper;

public class ServerData
extends Wrapper {
    public ServerData(Object wrappedObject) {
        super(wrappedObject);
    }

    public String getServerIp() {
        return MServerData.getServerIp(ServerData.sonyVegasInstance.getMappingsMapperCompat().ht, this.I);
    }
}
