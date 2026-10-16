package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MInventoryListBridge;
import vegas.sony.wrapper.Wrapper;
import java.util.List;

public class InventoryListBridge
extends Wrapper {
    public List getSlots() {
        return MInventoryListBridge.getSlots(InventoryListBridge.sonyVegasInstance.getMappingsMapperCompat().equipmentSlotGroup, this.I);
    }

    public InventoryListBridge(Object handle) {
        super(handle);
    }

    public static InventoryListBridge armor() {
        return new InventoryListBridge(MInventoryListBridge.getArmor(InventoryListBridge.sonyVegasInstance.getMappingsMapperCompat().equipmentSlotGroup));
    }
}
