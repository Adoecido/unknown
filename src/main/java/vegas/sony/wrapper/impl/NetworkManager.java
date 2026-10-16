package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MNetworkManager;
import vegas.sony.wrapper.Wrapper;

public class NetworkManager
extends Wrapper {

    public void G(Packet packet) {
        if (ForgeVersion.MC_1_20_6.d()) {
            MNetworkManager.m(NetworkManager.sonyVegasInstance.getMappingsMapperCompat().Do, this.I, packet.getObject(), null, true);
            return;
        }
        MNetworkManager.B(NetworkManager.sonyVegasInstance.getMappingsMapperCompat().Do, this.I, packet.getObject());
    }

    public EntityFishHookState getChannel() {
        return new EntityFishHookState(MNetworkManager.getChannel(NetworkManager.sonyVegasInstance.getMappingsMapperCompat().Do, this.I));
    }

    public NetworkPacketHandle c() {
        return new NetworkPacketHandle(MNetworkManager.U(NetworkManager.sonyVegasInstance.getMappingsMapperCompat().Do, this.I));
    }

    public NetworkManager(Object object) {
        super(object);
    }
}

