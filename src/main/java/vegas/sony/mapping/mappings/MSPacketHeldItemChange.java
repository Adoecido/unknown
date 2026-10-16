package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MSPacketHeldItemChange
extends Mapping {
    private final MappingMethod closeWindowPacketConstructor;

    public MSPacketHeldItemChange() {
        super(MappedClasses.l7);
        this.closeWindowPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Integer.TYPE});
    }

    public Object createCloseWindowPacket(int windowId) {
        return this.closeWindowPacketConstructor.newInstance(windowId);
    }
}

