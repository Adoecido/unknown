package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MContainer;
import vegas.sony.wrapper.Wrapper;

import java.util.ArrayList;
import java.util.List;

public class Container
extends Wrapper {
    public Slot getSlot(int slotIndex) {
        return new Slot(MContainer.getSlot(Container.sonyVegasInstance.getMappings().container, this.I, slotIndex));
    }

    public Container(Object object) {
        super(object);
    }

    public ItemStack getCarried() {
        return new ItemStack(MContainer.getCarried(Container.sonyVegasInstance.getMappings().container, this.I));
    }

    public List<Slot> getInventorySlots() {
        List slotHandles = Container.sonyVegasInstance.getMappings().container.getSlots(this.I);
        ArrayList<Slot> slots = new ArrayList<Slot>();
        for (Object slotHandle : slotHandles) {
            slots.add(new Slot(slotHandle));
        }
        return slots;
    }

    public int getWindowId() {
        return Container.sonyVegasInstance.getMappings().container.getWindowId(this.I);
    }
}
