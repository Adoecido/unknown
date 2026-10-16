package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ClientSettingsFrame;

public class ClientSettingsFrameResetGuiLayoutClickHandler
implements GuiClickListener {
    final ClientSettingsFrame j;

    @Override
    public void onPrimaryClick() {
        ClientSettingsFrame.n(this.j);
    }

    public ClientSettingsFrameResetGuiLayoutClickHandler(ClientSettingsFrame clientSettingsFrame) {
        this.j = clientSettingsFrame;
    }
}
