package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MIInventory;
import vegas.sony.wrapper.Wrapper;

public class Inventory
extends Wrapper {
    public int getSizeInventory() {
        return MIInventory.N(Inventory.sonyVegasInstance.getMappingsMapperCompat().X, this.I);
    }

    public ItemStack getStackInSlot(int n) {
        return new ItemStack(MIInventory.W(Inventory.sonyVegasInstance.getMappingsMapperCompat().X, this.I, n));
    }

    public Inventory(Object object) {
        super(object);
    }

    public boolean hasCustomInventoryName() {
        if (ForgeVersion.MC_1_7_10.Y()) {
            if (ForgeVersion.MC_1_16_5.d()) {
                return false;
            }
            return new WorldNameable(this.I).hasCustomName();
        }
        return MIInventory.e(Inventory.sonyVegasInstance.getMappingsMapperCompat().X, this.I);
    }


    public String getName() {
        if (ForgeVersion.MC_1_7_10.Y()) {
            return new WorldNameable(this.I).getDisplayName().a();
        }
        return MIInventory.F(Inventory.sonyVegasInstance.getMappingsMapperCompat().X, this.I);
    }
}

