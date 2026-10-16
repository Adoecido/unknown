package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MNetHandlerPlayClientImpl;

import java.util.Collection;

public class NetHandlerPlayClientImpl
extends NetHandlerPlayClient {

    public Collection getPlayerInfoMap() {
        return MNetHandlerPlayClientImpl.H(NetHandlerPlayClientImpl.sonyVegasInstance.getMappings().hB, this.I);
    }

    public boolean M() {
        return MNetHandlerPlayClientImpl.d(NetHandlerPlayClientImpl.sonyVegasInstance.getMappings().hB, this.I);
    }

    public void addToSendQueue(Packet packet) {
        MNetHandlerPlayClientImpl.I(NetHandlerPlayClientImpl.sonyVegasInstance.getMappings().hB, this.I, packet.getObject());
    }

    public boolean d() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return true;
        }
        return MNetHandlerPlayClientImpl.X(NetHandlerPlayClientImpl.sonyVegasInstance.getMappings().hB, this.I);
    }

    public NetHandlerPlayClientImpl(Object object) {
        super(object);
    }

    public NetworkManager a() {
        return new NetworkManager(MNetHandlerPlayClientImpl.z(NetHandlerPlayClientImpl.sonyVegasInstance.getMappings().hB, this.I));
    }
}

