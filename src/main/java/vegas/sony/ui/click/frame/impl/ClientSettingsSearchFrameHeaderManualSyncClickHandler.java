package vegas.sony.ui.click.frame.impl;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrameHeader;

class ClientSettingsSearchFrameHeaderManualSyncClickHandler
implements GuiClickListener {
    final ClientSettingsSearchFrameHeader z;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getSyncThread().requestSave();
        ClientSettingsSearchFrameHeader.F(this.z).setVisible(false);
    }

    ClientSettingsSearchFrameHeaderManualSyncClickHandler(ClientSettingsSearchFrameHeader clientSettingsSearchFrameHeader) {
        this.z = clientSettingsSearchFrameHeader;
    }
}
