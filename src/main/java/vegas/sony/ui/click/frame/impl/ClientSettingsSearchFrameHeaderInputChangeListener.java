package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiKeyTypedListener;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrame;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrameHeader;

public class ClientSettingsSearchFrameHeaderInputChangeListener
implements GuiKeyTypedListener {
    final ClientSettingsSearchFrameHeader W;
    final ClientSettingsSearchFrame y;


    @Override
    public void onKeyTyped(char c, int n) {
        if (ClientSettingsSearchFrameHeader.b(this.W) != ClientSettingsSearchFrameHeader.j(this.W).getText().length()) {
            this.y.K$src$V$1nbah4f();
            ClientSettings.activeTooltips = null;
        }
        ClientSettingsSearchFrameHeader.L(this.W, ClientSettingsSearchFrameHeader.j(this.W).getText().length());
    }

    public ClientSettingsSearchFrameHeaderInputChangeListener(ClientSettingsSearchFrameHeader clientSettingsSearchFrameHeader, ClientSettingsSearchFrame clientSettingsSearchFrame) {
        this.W = clientSettingsSearchFrameHeader;
        this.y = clientSettingsSearchFrame;
    }
}
