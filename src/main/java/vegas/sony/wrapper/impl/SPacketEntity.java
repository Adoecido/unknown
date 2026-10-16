package vegas.sony.wrapper.impl;

public class SPacketEntity
extends Packet {
    public int getZ() {
        if (ForgeVersion.MC_1_21_4.d()) {
            PositionMoveRotation change = this.getChange();
            return (int)change.getPosition().getZ();
        }
        return SPacketEntity.sonyVegasInstance.getMappingsMapperCompat().qR.getZ(this.I);
    }

    public SPacketEntity(Object handle) {
        super(handle);
    }

    public int getX() {
        if (ForgeVersion.MC_1_21_4.d()) {
            PositionMoveRotation change = this.getChange();
            return (int)change.getPosition().getX();
        }
        return SPacketEntity.sonyVegasInstance.getMappingsMapperCompat().qR.getX(this.I);
    }

    public byte getYaw() {
        if (ForgeVersion.MC_1_21_4.d()) {
            PositionMoveRotation change = this.getChange();
            return (byte)change.getYaw();
        }
        return SPacketEntity.sonyVegasInstance.getMappingsMapperCompat().qR.getYaw(this.I);
    }

    public int getY() {
        if (ForgeVersion.MC_1_21_4.d()) {
            PositionMoveRotation change = this.getChange();
            return (int)change.getPosition().getY();
        }
        return SPacketEntity.sonyVegasInstance.getMappingsMapperCompat().qR.getY(this.I);
    }

    public byte getPitch() {
        if (ForgeVersion.MC_1_21_4.d()) {
            PositionMoveRotation change = this.getChange();
            return (byte)change.getPitch();
        }
        return SPacketEntity.sonyVegasInstance.getMappingsMapperCompat().qR.getPitch(this.I);
    }

    public PositionMoveRotation getChange() {
        return new PositionMoveRotation(SPacketEntity.sonyVegasInstance.getMappingsMapperCompat().qR.getChange(this.I));
    }

    public int getEntityId() {
        return SPacketEntity.sonyVegasInstance.getMappingsMapperCompat().qR.getEntityId(this.I);
    }
}
