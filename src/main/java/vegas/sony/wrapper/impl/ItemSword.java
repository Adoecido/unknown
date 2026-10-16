package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MItemSword;

public class ItemSword
extends Item {
    private static final String b = "Removed";

    private static UnsupportedOperationException a(UnsupportedOperationException unsupportedOperationException) {
        return unsupportedOperationException;
    }

    public float Q$src$F$vp4c40() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return 0.0f;
        }
        if (ForgeVersion.MC_1_20_6.d()) {
            return MItemSword.c(ItemSword.sonyVegasInstance.getMappings().qF, this.s().getObject());
        }
        return MItemSword.c(ItemSword.sonyVegasInstance.getMappings().qF, this.I);
    }

    public ItemSword(Object object) {
        super(object);
        if (ForgeVersion.MC_1_21_5.d()) {
            throw new UnsupportedOperationException(b);
        }
    }

    public float R() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return 0.0f;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            return MItemSword.T(ItemSword.sonyVegasInstance.getMappings().qF, this.I);
        }
        return MItemSword.X(ItemSword.sonyVegasInstance.getMappings().qF, this.I);
    }

    public ToolMaterial s() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return null;
        }
        return new ToolMaterial(MItemSword.x(ItemSword.sonyVegasInstance.getMappings().qF, this.I));
    }
}

