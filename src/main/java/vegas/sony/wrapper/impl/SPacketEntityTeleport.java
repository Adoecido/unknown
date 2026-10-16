package vegas.sony.wrapper.impl;

public class SPacketEntityTeleport
extends Packet {
    public SPacketEntityTeleport(Object handle) {
        super(handle);
    }

    public int getZ() {
        return SPacketEntityTeleport.sonyVegasInstance.getMappings().qH.getZ(this.I);
    }

    public byte getYaw() {
        return SPacketEntityTeleport.sonyVegasInstance.getMappings().qH.getYaw(this.I);
    }

    public byte getPitch() {
        return SPacketEntityTeleport.sonyVegasInstance.getMappings().qH.getPitch(this.I);
    }

    public int getY() {
        return SPacketEntityTeleport.sonyVegasInstance.getMappings().qH.getY(this.I);
    }

    public int getEntityId() {
        return SPacketEntityTeleport.sonyVegasInstance.getMappings().qH.getEntityId(this.I);
    }

    public int getX() {
        return SPacketEntityTeleport.sonyVegasInstance.getMappings().qH.getX(this.I);
    }
}
