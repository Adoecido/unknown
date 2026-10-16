package vegas.sony.wrapper.impl;

public class C03PacketPlayer
extends Packet {
    public double getX() {
        return C03PacketPlayer.sonyVegasInstance.getMappings().Dq.getX(this.I);
    }

    public float getPitch() {
        return C03PacketPlayer.sonyVegasInstance.getMappings().Dq.getPitch(this.I);
    }

    public double getY() {
        return C03PacketPlayer.sonyVegasInstance.getMappings().Dq.getY(this.I);
    }

    public float getYaw() {
        return C03PacketPlayer.sonyVegasInstance.getMappings().Dq.getYaw(this.I);
    }

    public C03PacketPlayer(Object handle) {
        super(handle);
    }

    public static C03PacketPlayer create(boolean onGround) {
        if (ForgeVersion.MC_1_21_4.d()) {
            EntityPlayerSP player = Minecraft.thePlayer();
            return CPacketPlayer.create(onGround, player.r());
        }
        if (ForgeVersion.MC_1_20_6.d()) {
            EntityPlayerSP player = Minecraft.thePlayer();
            return new C03PacketPlayer(C03PacketPlayer.sonyVegasInstance.getMappings().Dq.createPacket(player.z(), player.N(), player.h(), player.J(), player.V(), onGround, false, false));
        }
        return new C03PacketPlayer(C03PacketPlayer.sonyVegasInstance.getMappings().Dq.createPacket(onGround));
    }

    public double getZ() {
        return C03PacketPlayer.sonyVegasInstance.getMappings().Dq.getZ(this.I);
    }

    public boolean hasPosition() {
        return C03PacketPlayer.sonyVegasInstance.getMappings().Dq.hasPosition(this.I);
    }

    public boolean hasRotation() {
        return C03PacketPlayer.sonyVegasInstance.getMappings().Dq.hasRotation(this.I);
    }

    public void setPitch(float pitch) {
        C03PacketPlayer.sonyVegasInstance.getMappings().Dq.setPitch(this.I, pitch);
    }

    public void setYaw(float yaw) {
        C03PacketPlayer.sonyVegasInstance.getMappings().Dq.setYaw(this.I, yaw);
    }

    public void setHasRotation(boolean value) {
        C03PacketPlayer.sonyVegasInstance.getMappings().Dq.setHasRotation(this.I, value);
    }
}
