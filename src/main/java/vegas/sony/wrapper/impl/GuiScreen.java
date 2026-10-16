package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MGuiScreen;
import vegas.sony.module.none.ClientSettings;

public class GuiScreen
extends Screen {
    private static boolean r;

    public static void Q(boolean bl) {
        r = bl;
    }

    public int g() {
        return MGuiScreen.U(GuiScreen.sonyVegasInstance.getMappings().h1, this.I);
    }

    public static boolean p$src$Z$8062rc() {
        return r;
    }

    public int k() {
        return MGuiScreen.D(GuiScreen.sonyVegasInstance.getMappings().h1, this.I);
    }

    public static boolean Z() {
        boolean bl = GuiScreen.p$src$Z$8062rc();
        return !bl;
    }


    @Override
    public boolean isNull() {
        if (!ClientSettings.INSTANCE.inputEnabled) {
            return false;
        }
        return super.isNull();
    }

    public GuiScreen(Object object) {
        super(object);
    }

    public ITextComponent F() {
        return new ITextComponent(GuiScreen.sonyVegasInstance.getMappings().h1.S(this.I));
    }

    static {
        if (GuiScreen.Z()) {
            GuiScreen.Q(true);
        }
    }
}

