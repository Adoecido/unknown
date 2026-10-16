package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.wrapper.impl.Packet;

public class ShortStatePacketBridge
extends Packet {
    public short getTransactionId() {
        return SonyVegas.INSTANCE.getMappingsMapperCompat().F.getTransactionId(this.I);
    }

    public ShortStatePacketBridge(Object handle) {
        super(handle);
    }
}
