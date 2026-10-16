package vegas.sony.wrapper.impl;

public class SEntityPacket
extends Packet {
    public int getDeltaY() {
        return SEntityPacket.sonyVegasInstance.getMappingsMapperCompat().qC.getDeltaY(this.I);
    }

    public int getDeltaZ() {
        return SEntityPacket.sonyVegasInstance.getMappingsMapperCompat().qC.getDeltaZ(this.I);
    }

    public Entity getEntity(World world) {
        return new Entity(SEntityPacket.sonyVegasInstance.getMappingsMapperCompat().qC.getEntity(this.I, world.getObject()));
    }

    public SEntityPacket(Object handle) {
        super(handle);
    }

    public int getDeltaX() {
        return SEntityPacket.sonyVegasInstance.getMappingsMapperCompat().qC.getDeltaX(this.I);
    }
}

