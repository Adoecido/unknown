package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MSPacketDestroyEntitiesArrayBridge
extends Mapping {
    private MappingField entityIdsField;

    public Object getEntityIdsContainer(Object packet) {
        return this.entityIdsField.getObject(packet);
    }

    public MSPacketDestroyEntitiesArrayBridge() {
        this(MSPacketEntityVelocity.getPacketMappingControlFlowState());
    }

    private MSPacketDestroyEntitiesArrayBridge(int[] controlFlowState) {
        super(MappedClasses.Yv);
        if (controlFlowState != null) {
            if (ForgeVersion.MC_1_7_10.L()) {
                this.entityIdsField = this.J("field_149100_a", Wrapper.isNativeAvailable, int[].class);
            } else if (ForgeVersion.MC_1_17.d()) {
                this.entityIdsField = this.J("entityIds", true, MappedClasses.N);
            } else {
                this.entityIdsField = this.J("entityIDs", true, int[].class);
            }
            return;
        }
        if (ForgeVersion.MC_1_7_10.L()) {
            this.entityIdsField = this.J("entityIds", true, MappedClasses.N);
        }
        this.entityIdsField = this.J("entityIDs", true, int[].class);
    }

    public int[] getEntityIds(Object packet) {
        return this.entityIdsField.getIntArray(packet);
    }

}
