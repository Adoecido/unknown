package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MSPacketEntityVelocity;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MBytePacketFieldBridge
extends Mapping {
    private final MappingField headYawField;

    public MBytePacketFieldBridge() {
        this(MSPacketEntityVelocity.getPacketMappingControlFlowState());
    }

    private MBytePacketFieldBridge(int[] controlFlowState) {
        super(MappedClasses.D7);
        if (controlFlowState != null) {
            if (ForgeVersion.MC_1_16_5.d()) {
                this.headYawField = this.J("yHeadRot", true, Byte.TYPE);
            } else if (ForgeVersion.MC_1_7_10.L()) {
                this.headYawField = this.J("field_149383_b", true, Byte.TYPE);
            } else {
                this.headYawField = this.J("yaw", true, Byte.TYPE);
            }
            return;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            this.J("field_149383_b", true, Byte.TYPE);
        }
        this.headYawField = this.J("yaw", true, Byte.TYPE);
    }

    public byte getHeadYaw(Object packet) {
        return (byte)this.headYawField.getInt(packet);
    }

}
