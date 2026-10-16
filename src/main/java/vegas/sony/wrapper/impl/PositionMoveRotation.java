package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class PositionMoveRotation
extends Wrapper {
    public Vec3 getDeltaMovement() {
        return new Vec3(PositionMoveRotation.sonyVegasInstance.getMappingsMapperCompat().qT.getDeltaMovement(this.I));
    }

    public void setPitch(float pitch) {
        PositionMoveRotation.sonyVegasInstance.getMappingsMapperCompat().qT.setPitch(this.I, pitch);
    }

    public void setYaw(float yaw) {
        PositionMoveRotation.sonyVegasInstance.getMappingsMapperCompat().qT.setYaw(this.I, yaw);
    }

    public Vec3 getPosition() {
        return new Vec3(PositionMoveRotation.sonyVegasInstance.getMappingsMapperCompat().qT.getPosition(this.I));
    }

    public float getPitch() {
        return PositionMoveRotation.sonyVegasInstance.getMappingsMapperCompat().qT.getPitch(this.I);
    }

    public float getYaw() {
        return PositionMoveRotation.sonyVegasInstance.getMappingsMapperCompat().qT.getYaw(this.I);
    }

    public PositionMoveRotation(Object handle) {
        super(handle);
    }
}
