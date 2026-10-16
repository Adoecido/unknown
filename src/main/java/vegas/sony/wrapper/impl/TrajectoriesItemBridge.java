package vegas.sony.wrapper.impl;

public class TrajectoriesItemBridge
extends Item {
    public static boolean isCharged(ItemStack itemStack) {
        return TrajectoriesItemBridge.sonyVegasInstance.getMappingsMapperCompat().RX.isCharged(itemStack.getObject());
    }

    public TrajectoriesItemBridge(Object handle) {
        super(handle);
    }
}
