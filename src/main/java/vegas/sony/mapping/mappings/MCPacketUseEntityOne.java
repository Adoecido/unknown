package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MCPacketUseEntityOne
extends Mapping {
    private final MappingMethod playerAbilitiesPacketConstructor;

    public MCPacketUseEntityOne() {
        super(MappedClasses.z1);
        this.playerAbilitiesPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{MappedClasses.q9});
    }

    public Object createPlayerAbilitiesPacket(Object playerCapabilities) {
        return this.playerAbilitiesPacketConstructor.newInstance(playerCapabilities);
    }
}

