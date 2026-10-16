package vegas.sony.wrapper.impl;

public class SPacketBlockChange
extends Packet {
    public SPacketBlockChange(Object handle) {
        super(handle);
    }

    public int getLegacyX() {
        return SPacketBlockChange.sonyVegasInstance.getMappings().CN.getLegacyX(this.I);
    }

    public BlockState getBlockState() {
        return new BlockState(SPacketBlockChange.sonyVegasInstance.getMappings().CN.getBlockState(this.I));
    }

    public BlockPos getBlockPosition() {
        return new BlockPos(SPacketBlockChange.sonyVegasInstance.getMappings().CN.getBlockPosition(this.I));
    }

    public int getLegacyY() {
        return SPacketBlockChange.sonyVegasInstance.getMappings().CN.getLegacyY(this.I);
    }

    public int getLegacyZ() {
        return SPacketBlockChange.sonyVegasInstance.getMappings().CN.getLegacyZ(this.I);
    }
}
