package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrame;
import vegas.sony.ui.click.frame.impl.ClientSettingsSectionFrame;

class ClientSettingsSectionFrameCloseSearchClickHandler
implements GuiClickListener {
    final ClientSettingsSectionFrame V;

    @Override
    public void onPrimaryClick() {
        ClientSettings.setFrameVisibility(ClientSettingsSearchFrame.class, false);
    }

    ClientSettingsSectionFrameCloseSearchClickHandler(ClientSettingsSectionFrame clientSettingsSectionFrame) {
        this.V = clientSettingsSectionFrame;
    }
}
