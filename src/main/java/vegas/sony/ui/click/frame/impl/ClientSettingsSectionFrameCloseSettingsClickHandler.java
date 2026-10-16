package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ClientSettingsFrame;
import vegas.sony.ui.click.frame.impl.ClientSettingsSectionFrame;

class ClientSettingsSectionFrameCloseSettingsClickHandler
implements GuiClickListener {
    final ClientSettingsSectionFrame J;

    ClientSettingsSectionFrameCloseSettingsClickHandler(ClientSettingsSectionFrame clientSettingsSectionFrame) {
        this.J = clientSettingsSectionFrame;
    }

    @Override
    public void onPrimaryClick() {
        ClientSettings.setFrameVisibility(ClientSettingsFrame.class, false);
        this.J.t(false, false);
    }
}
