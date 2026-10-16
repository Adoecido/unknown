package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MSlot;
import vegas.sony.wrapper.Wrapper;

public class Slot
extends Wrapper {
    public Slot(Object object) {
        super(object);
    }


    public int getSlotIndex() {
        return MSlot.getSlotIndex(Slot.sonyVegasInstance.getMappingsMapperCompat().slot, this.getObject());
    }

    public Inventory getInventory() {
        return new Inventory(MSlot.getInventoryOrContainer(Slot.sonyVegasInstance.getMappingsMapperCompat().slot, this.getObject()));
    }

    public int getSlotNumber() {
        return MSlot.getSlotNumber(Slot.sonyVegasInstance.getMappingsMapperCompat().slot, this.getObject());
    }

    public ItemStack getStack() {
        return new ItemStack(Slot.sonyVegasInstance.getMappingsMapperCompat().slot.getStack(this.getObject()));
    }

    public boolean hasStack() {
        if (ForgeVersion.MC_26_1.d()) {
            boolean hasStack = !this.getStack().isNull();
            return hasStack;
        }
        boolean hasStack = Slot.sonyVegasInstance.getMappingsMapperCompat().slot.getStack(this.getObject()) != null;
        return hasStack;
    }
}

