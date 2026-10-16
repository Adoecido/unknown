package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MGuiContainer;

public class GuiContainer
extends GuiScreen {
    public static ResourceLocation m$src$Lvegas_sony_wrapper_impl_ResourceLocation_$1fc62cj() {
        return new ResourceLocation(GuiContainer.sonyVegasInstance.getMappings().hK.v());
    }

    public Slot P() {
        return new Slot(MGuiContainer.y(GuiContainer.sonyVegasInstance.getMappings().hK, this.I));
    }

    public int v() {
        return MGuiContainer.G(GuiContainer.sonyVegasInstance.getMappings().hK, this.I);
    }

    public int p() {
        return MGuiContainer.B(GuiContainer.sonyVegasInstance.getMappings().hK, this.I);
    }

    public int x() {
        return MGuiContainer.i(GuiContainer.sonyVegasInstance.getMappings().hK, this.I);
    }

    public Slot getSlotAtPosition(int n, int n2) {
        return new Slot(MGuiContainer.a(GuiContainer.sonyVegasInstance.getMappings().hK, this.I, n, n2));
    }

    public int b() {
        return MGuiContainer.e(GuiContainer.sonyVegasInstance.getMappings().hK, this.I);
    }

    public GuiContainer(Object object) {
        super(object);
    }

    public Container getInventorySlots() {
        return new Container(GuiContainer.sonyVegasInstance.getMappings().hK.y(this.I));
    }
}

