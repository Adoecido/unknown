package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class WorldNameable
extends Wrapper {
    public ITextComponent getDisplayName() {
        return new ITextComponent(WorldNameable.sonyVegasInstance.getMappingsMapperCompat().Dm.A(this.I));
    }

    public boolean hasCustomName() {
        return WorldNameable.sonyVegasInstance.getMappingsMapperCompat().Dm.V(this.I);
    }

    public String getName() {
        if (ForgeVersion.MC_1_16_5.d()) {
            ITextComponent iTextComponent = new ITextComponent(WorldNameable.sonyVegasInstance.getMappingsMapperCompat().Dm.s(this.I));
            return iTextComponent.getFormattedText();
        }
        return WorldNameable.sonyVegasInstance.getMappingsMapperCompat().Dm.L(this.I);
    }

    public WorldNameable(Object object) {
        super(object);
    }
}
