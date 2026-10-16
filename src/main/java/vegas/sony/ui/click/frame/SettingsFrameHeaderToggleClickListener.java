package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.SettingsFrameHeaderComponent;

class SettingsFrameHeaderToggleClickListener
implements GuiClickListener {
    final SettingsFrameHeaderComponent i;

    SettingsFrameHeaderToggleClickListener(SettingsFrameHeaderComponent settingsFrameHeaderComponent) {
        this.i = settingsFrameHeaderComponent;
    }

    @Override
    public void onPrimaryClick() {
        this.i.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0().c(!this.i.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0().y$src$Z$1f55jvh());
    }

}

