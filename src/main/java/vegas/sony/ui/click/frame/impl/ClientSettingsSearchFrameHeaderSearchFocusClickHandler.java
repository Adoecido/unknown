package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrameHeader;

class ClientSettingsSearchFrameHeaderSearchFocusClickHandler
implements GuiClickListener {
    final ClientSettingsSearchFrameHeader q;

    @Override
    public void onPrimaryClick() {
        this.q.V$src$V$enocyv();
    }

    ClientSettingsSearchFrameHeaderSearchFocusClickHandler(ClientSettingsSearchFrameHeader clientSettingsSearchFrameHeader) {
        this.q = clientSettingsSearchFrameHeader;
    }
}
