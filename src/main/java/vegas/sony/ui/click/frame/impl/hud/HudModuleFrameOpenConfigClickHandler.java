package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.impl.hud.AnchoredHudModuleConfigFrame;

class HudModuleFrameOpenConfigClickHandler
implements GuiClickListener {
    private final AnchoredHudModuleConfigFrame configFrame;


    @Override
    public void onPrimaryClick() {
        this.configFrame.setVisible(false);
        for (GuiComponent guiComponent : this.configFrame.f()) {
            if (guiComponent == this.configFrame.j$src$Lvegas_sony_ui_click_frame_FrameHeaderComponent_$175vsfc()) continue;
            guiComponent.setVisible(false);
        }
    }

    HudModuleFrameOpenConfigClickHandler(AnchoredHudModuleConfigFrame anchoredHudModuleConfigFrame) {
        this.configFrame = anchoredHudModuleConfigFrame;
    }
}

