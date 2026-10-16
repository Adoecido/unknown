package vegas.sony.wrapper.impl;

public class CPacketPlayerDigging
extends Packet {
    public PlayerDiggingAction getAction() {
        return new PlayerDiggingAction(CPacketPlayerDigging.sonyVegasInstance.getMappingsMapperCompat().qA.getAction(this.I));
    }

    public int getLegacyActionId() {
        return CPacketPlayerDigging.sonyVegasInstance.getMappingsMapperCompat().qA.getLegacyActionId(this.I);
    }

    public boolean isReleaseUseItem() {
        return ForgeVersion.MC_1_8_9.d() ? this.getAction().equals(PlayerDiggingAction.releaseUseItem()) : this.getLegacyActionId() == 5;
    }

    public BlockPos getPosition() {
        return new BlockPos(CPacketPlayerDigging.sonyVegasInstance.getMappingsMapperCompat().qA.getPosition(this.I));
    }

    public EnumFacing getFacing() {
        return new EnumFacing(CPacketPlayerDigging.sonyVegasInstance.getMappingsMapperCompat().qA.getFacing(this.I));
    }

    public CPacketPlayerDigging(Object handle) {
        super(handle);
    }
}

