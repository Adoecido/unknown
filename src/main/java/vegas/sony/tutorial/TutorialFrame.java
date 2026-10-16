package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;

public class TutorialFrame
extends Frame {
    private static final EncString eb;
    private static GuiComponent[] zb;

    public static GuiComponent[] l$src$ALvegas_sony_ui_click_component_GuiComponent_$xp34x4() {
        return zb;
    }

    @Override
    public void Y() {
        this.l$src$V$1mibm4x();
        SonyVegas.INSTANCE.getTutorialManager().refreshCurrentAction();
        ClientSettings.queueFrameOpen(this);
    }

    public static void n(GuiComponent[] guiComponentArray) {
        zb = guiComponentArray;
    }

    @Override
    public void c() {
        super.c();
    }

    static {
        TutorialFrame.n(new GuiComponent[4]);
        eb = Str.enc("tutorial");
    }

    @Override
    public void v() {
    }

    @Override
    public String getName() {
        return eb.get();
    }
}
