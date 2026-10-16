package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.impl.ClientSettingsFrame;
import vegas.sony.ui.click.frame.impl.ClientSettingsSectionFrame;

public class ClientSettingsFrameActionClickHandler
implements GuiClickListener {
    final ClientSettingsFrame p;
    final String k;

    @Override
    public void onPrimaryClick() {
        ClientSettingsSectionFrame clientSettingsSectionFrame = ClientSettingsFrame.m(this.p, this.k);
        clientSettingsSectionFrame.addChildren((GuiComponent[])ClientSettingsFrame.t(this.p).get(this.k));
    }

    public ClientSettingsFrameActionClickHandler(ClientSettingsFrame sG2, String string) {
        this.p = sG2;
        this.k = string;
    }
}
