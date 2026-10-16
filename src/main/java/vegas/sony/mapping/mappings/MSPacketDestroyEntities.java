package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MMappedClassSlotNRegistration;

public class MSPacketDestroyEntities
extends Mapping {
    private final MappingField entityIdsField;

    public int[] getEntityIds(Object packet) {
        return this.entityIdsField.getIntArray(packet);
    }

    public MSPacketDestroyEntities() {
        this(MMappedClassSlotNRegistration.r());
    }

    private MSPacketDestroyEntities(int controlFlowState) {
        super(MappedClasses.qc);
        this.entityIdsField = this.J("a", false, int[].class);
    }

}

