package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.hud.HudModuleFrameBase;

public class HudModuleFrameCloseClickHandler
implements GuiClickListener {
    private final HudModuleFrameBase frame;

    @Override
    public void onPrimaryClick() {
        this.frame.closeHudFrame();
    }

    public HudModuleFrameCloseClickHandler(HudModuleFrameBase hudModuleFrameBase) {
        this.frame = hudModuleFrameBase;
    }
}
