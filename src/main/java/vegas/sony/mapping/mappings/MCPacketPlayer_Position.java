package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MPacketIdFactory;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MCPacketPlayer_Position
extends Mapping {
    private MappingMethod positionPacketConstructor;

    public MCPacketPlayer_Position() {
        this(MPacketIdFactory.getPacketMappingControlFlowState());
    }

    private MCPacketPlayer_Position(GuiComponent[] controlFlowState) {
        super(MappedClasses.ul);
        if (controlFlowState != null) {
            if (ForgeVersion.MC_1_7_10.Y()) {
                if (ForgeVersion.MC_1_21_4.d()) {
                    this.positionPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE, Boolean.TYPE});
                } else {
                    this.positionPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE});
                }
            } else {
                this.positionPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE});
            }
            return;
        }
        if (ForgeVersion.MC_1_7_10.Y()) {
            this.positionPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE, Boolean.TYPE});
        }
        this.positionPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE});
        this.positionPacketConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE});
    }

    public Object createPositionPacket(double x, double y, double z, boolean onGround, boolean horizontalCollision) {
        return this.positionPacketConstructor.newInstance(x, y, z, onGround, horizontalCollision);
    }

    public Object createPositionPacket(double x, double y, double z, boolean onGround) {
        return this.positionPacketConstructor.newInstance(x, y, z, onGround);
    }

    public Object createLegacyPositionPacket(double x, double feetY, double stanceY, double z, boolean onGround) {
        return this.positionPacketConstructor.newInstance(x, feetY, stanceY, z, onGround);
    }
}

