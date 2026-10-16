package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MCPacketPlayer
extends Mapping {
    private final MappingField zField;
    private final MappingField hasPositionField;
    private final MappingMethod packetConstructor;
    private final MappingField pitchField;
    private final MappingField rotatingField;
    private final MappingField xField;
    private final MappingField yField;
    private final MappingField yawField;

    public MCPacketPlayer() {
        this(MPacketIdFactory.getPacketMappingControlFlowState());
    }

    private MCPacketPlayer(GuiComponent[] controlFlowState) {
        super(MappedClasses.qD);
        if (ForgeVersion.MC_1_21_4.d()) {
            this.packetConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE});
        } else if (ForgeVersion.MC_1_20_6.d()) {
            this.packetConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE});
        } else {
            this.packetConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{Boolean.TYPE});
        }
        if (ForgeVersion.MC_1_7_10.L()) {
            if (Wrapper.sonyVegasInstance.isVanillaMinecraftPresent()) {
                this.xField = this.J("x", true, Double.TYPE);
                this.yField = this.J("y", true, Double.TYPE);
                this.zField = this.J("z", true, Double.TYPE);
                this.yawField = this.J("yaw", true, Float.TYPE);
                this.pitchField = this.J("pitch", true, Float.TYPE);
                this.rotatingField = this.J("rotating", true, Boolean.TYPE);
            } else {
                this.xField = this.J("field_149479_a", Wrapper.isNativeAvailable, Double.TYPE);
                this.yField = this.J("field_149477_b", Wrapper.isNativeAvailable, Double.TYPE);
                this.zField = this.J("field_149475_d", Wrapper.isNativeAvailable, Double.TYPE);
                this.yawField = this.J("field_149476_e", Wrapper.isNativeAvailable, Float.TYPE);
                this.pitchField = this.J("field_149473_f", Wrapper.isNativeAvailable, Float.TYPE);
                this.rotatingField = this.J("field_149478_c", Wrapper.isNativeAvailable, Boolean.TYPE);
            }
            this.hasPositionField = this.J("field_149480_h", Wrapper.isNativeAvailable, Boolean.TYPE);
        } else {
            this.xField = this.J("x", true, Double.TYPE);
            this.yField = this.J("y", true, Double.TYPE);
            this.zField = this.J("z", true, Double.TYPE);
            if (ForgeVersion.MC_1_16_5.d()) {
                this.yawField = this.J("yRot", true, Float.TYPE);
                this.pitchField = this.J("xRot", true, Float.TYPE);
            } else {
                this.yawField = this.J("yaw", true, Float.TYPE);
                this.pitchField = this.J("pitch", true, Float.TYPE);
            }
            this.hasPositionField = this.J("moving", true, Boolean.TYPE);
            this.rotatingField = this.J("rotating", true, Boolean.TYPE);
        }
    }

    public Object createPacket(boolean onGround) {
        return this.packetConstructor.newInstance(onGround);
    }

    public Object createPacket(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean hasPosition, boolean hasRotation) {
        return this.packetConstructor.newInstance(x, y, z, Float.valueOf(yaw), Float.valueOf(pitch), onGround, hasPosition, hasRotation);
    }

    public boolean hasPosition(Object packet) {
        return this.hasPositionField.getBoolean(packet);
    }

    public boolean hasRotation(Object packet) {
        return this.rotatingField.getBoolean(packet);
    }

    public void setPitch(Object packet, float pitch) {
        this.pitchField.setFloat(packet, pitch);
    }

    public void setYaw(Object packet, float yaw) {
        this.yawField.setFloat(packet, yaw);
    }

    public float getPitch(Object packet) {
        return this.pitchField.getFloat(packet);
    }

    public float getYaw(Object packet) {
        return this.yawField.getFloat(packet);
    }

    public double getY(Object packet) {
        return this.yField.getDouble(packet);
    }

    public Object createPacket(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontalCollision, boolean hasPosition, boolean hasRotation) {
        return this.packetConstructor.newInstance(x, y, z, Float.valueOf(yaw), Float.valueOf(pitch), onGround, horizontalCollision, hasPosition, hasRotation);
    }

    public double getZ(Object packet) {
        return this.zField.getDouble(packet);
    }

    public void setHasRotation(Object packet, boolean value) {
        this.rotatingField.setBoolean(packet, value);
    }

    public double getX(Object packet) {
        return this.xField.getDouble(packet);
    }
}

