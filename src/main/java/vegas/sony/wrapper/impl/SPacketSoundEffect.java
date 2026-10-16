package vegas.sony.wrapper.impl;

public class SPacketSoundEffect
extends Packet {

    public SPacketSoundEffect(Object handle) {
        super(handle);
    }

    public String getParticleName() {
        if (ForgeVersion.MC_1_8_9.d()) {
            return new EnumParticleTypes(SPacketSoundEffect.sonyVegasInstance.getMappings().Cs.getParticleType(this.I)).K();
        }
        return SPacketSoundEffect.sonyVegasInstance.getMappings().Cs.getLegacyParticleName(this.I);
    }

    public double getZ() {
        return SPacketSoundEffect.sonyVegasInstance.getMappings().Cs.getZ(this.I);
    }

    public double getX() {
        return SPacketSoundEffect.sonyVegasInstance.getMappings().Cs.getX(this.I);
    }

    public double getY() {
        return SPacketSoundEffect.sonyVegasInstance.getMappings().Cs.getY(this.I);
    }
}

