package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SPacketExplosion
extends Wrapper {
    public void setPosition(float position) {
        SPacketExplosion.sonyVegasInstance.getMappingsMapperCompat().hQ.setPosition(this.I, position);
    }

    public void setPreviousSpeed(float previousSpeed) {
        SPacketExplosion.sonyVegasInstance.getMappingsMapperCompat().hQ.setPreviousSpeed(this.I, previousSpeed);
    }

    public float getSpeed() {
        return SPacketExplosion.sonyVegasInstance.getMappingsMapperCompat().hQ.getSpeed(this.I);
    }

    public float getPreviousSpeed() {
        return SPacketExplosion.sonyVegasInstance.getMappingsMapperCompat().hQ.getPreviousSpeed(this.I);
    }

    public SPacketExplosion(Object handle) {
        super(handle);
    }

    public float getPosition() {
        return SPacketExplosion.sonyVegasInstance.getMappingsMapperCompat().hQ.getPosition(this.I);
    }
}
