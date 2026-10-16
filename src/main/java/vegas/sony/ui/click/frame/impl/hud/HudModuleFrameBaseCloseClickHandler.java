package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.hud.HudModuleFrameBase;

public class HudModuleFrameBaseCloseClickHandler
implements GuiClickListener {
    private final HudModuleFrameBase frame;

    @Override
    public void onPrimaryClick() {
        this.frame.openAnchoredSettings();
    }

    public HudModuleFrameBaseCloseClickHandler(HudModuleFrameBase hudModuleFrameBase) {
        this.frame = hudModuleFrameBase;
    }
}
