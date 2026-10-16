package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.impl.SettingsFrameHeaderComponent;

class SettingsFrameHeaderPrimaryButtonClickHandler
implements GuiClickListener {
    final SettingsFrameHeaderComponent D;
    final Frame p;

    @Override
    public void onPrimaryClick() {
        if (SettingsFrameHeaderComponent.D(this.D) != null) {
            SettingsFrameHeaderComponent.D(this.D).onPrimaryClick();
        }
        ClientSettings.setFrameVisibility(this.p.getClass(), false);
    }

    SettingsFrameHeaderPrimaryButtonClickHandler(SettingsFrameHeaderComponent settingsFrameHeaderComponent, Frame frame) {
        this.D = settingsFrameHeaderComponent;
        this.p = frame;
    }

}

