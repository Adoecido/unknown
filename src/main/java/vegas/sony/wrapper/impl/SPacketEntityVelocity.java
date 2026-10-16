package vegas.sony.wrapper.impl;

public class SPacketEntityVelocity
extends Packet {
    private Vec3 getMovement() {
        return new Vec3(SPacketEntityVelocity.sonyVegasInstance.getMappings().s.getMovement(this.I));
    }

    public int getMotionZ() {
        if (this.I == null) {
            return 0;
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            return (int)(this.getMovement().getZ() * 8000.0);
        }
        return SPacketEntityVelocity.sonyVegasInstance.getMappings().s.getMotionZ(this.I);
    }

    public void setMotionZ(double motionZ) {
        if (this.I == null) {
            return;
        }
        if (ForgeVersion.MC_26_1.d()) {
            throw new UnsupportedOperationException("Use packet reconstruction for MC 26.1+");
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            Vec3 movement = this.getMovement();
            movement.Z(motionZ);
            SPacketEntityVelocity.sonyVegasInstance.getMappings().s.setMovement(this.I, movement.getObject());
            return;
        }
        SPacketEntityVelocity.sonyVegasInstance.getMappings().s.setMotionZ(this.I, (int)motionZ);
    }

    public int getMotionY() {
        if (this.I == null) {
            return 0;
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            return (int)(this.getMovement().getY() * 8000.0);
        }
        return SPacketEntityVelocity.sonyVegasInstance.getMappings().s.getMotionY(this.I);
    }

    public void setMotionX(double motionX) {
        if (this.I == null) {
            return;
        }
        if (ForgeVersion.MC_26_1.d()) {
            throw new UnsupportedOperationException("Use packet reconstruction for MC 26.1+");
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            Vec3 movement = this.getMovement();
            movement.N(motionX);
            SPacketEntityVelocity.sonyVegasInstance.getMappings().s.setMovement(this.I, movement.getObject());
            return;
        }
        SPacketEntityVelocity.sonyVegasInstance.getMappings().s.setMotionX(this.I, (int)motionX);
    }

    public int getMotionX() {
        if (this.I == null) {
            return 0;
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            return (int)(this.getMovement().getX() * 8000.0);
        }
        return SPacketEntityVelocity.sonyVegasInstance.getMappings().s.getMotionX(this.I);
    }

    public void setMotionY(double motionY) {
        if (this.I == null) {
            return;
        }
        if (ForgeVersion.MC_26_1.d()) {
            throw new UnsupportedOperationException("Use packet reconstruction for MC 26.1+");
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            Vec3 movement = this.getMovement();
            movement.m(motionY);
            SPacketEntityVelocity.sonyVegasInstance.getMappings().s.setMovement(this.I, movement.getObject());
            return;
        }
        SPacketEntityVelocity.sonyVegasInstance.getMappings().s.setMotionY(this.I, (int)motionY);
    }

    public SPacketEntityVelocity(Object handle) {
        super(handle);
    }

    public int getEntityId() {
        if (this.I == null) {
            return 0;
        }
        return SPacketEntityVelocity.sonyVegasInstance.getMappings().s.getEntityId(this.I);
    }

    private static UnsupportedOperationException propagateException(UnsupportedOperationException exception) {
        return exception;
    }
}
