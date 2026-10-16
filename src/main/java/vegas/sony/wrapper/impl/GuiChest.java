package vegas.sony.wrapper.impl;

public class GuiChest
extends GuiContainer {
    public String getTitle() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.F().getFormattedText();
        }
        return this.getLowerChestInventory().getName();
    }

    public GuiChest(Object chestScreenHandle) {
        super(chestScreenHandle);
    }

    public Inventory getLowerChestInventory() {
        if (ForgeVersion.MC_1_16_5.d()) {
            Object containerScreenHandle = GuiChest.sonyVegasInstance.getMappingsMapperCompat().hK.t(this.I);
            Object inventoryHandle = GuiChest.sonyVegasInstance.getMappingsMapperCompat().guiChest
                    .getLowerChestInventory(containerScreenHandle);
            return new Inventory(inventoryHandle);
        }
        return new Inventory(GuiChest.sonyVegasInstance.getMappingsMapperCompat().guiChest.getLowerChestInventory(this.I));
    }

}

