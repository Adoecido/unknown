package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MCPacketUseEntityActionPacket
extends Mapping {
    private static final String LOCATION_FIELD_NAME = "location";
    private final MappingField locationField;

    public MCPacketUseEntityActionPacket() {
        super(MappedClasses.uL);
        this.locationField = this.J(LOCATION_FIELD_NAME, true, MappedClasses.qP);
    }

    public Object getLocation(Object packet) {
        return this.locationField.getObject(packet);
    }
}

