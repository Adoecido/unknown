package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.impl.ClientSettingsFrame;
import vegas.sony.ui.click.frame.impl.ClientSettingsSectionFrame;

public class ClientSettingsFrameSectionOpenClickHandler
implements GuiClickListener {
    final String c;
    final ClientSettingsFrame I;

    @Override
    public void onPrimaryClick() {
        ClientSettingsSectionFrame clientSettingsSectionFrame = ClientSettingsFrame.m(this.I, this.c);
        clientSettingsSectionFrame.addChildren((GuiComponent[])ClientSettingsFrame.t(this.I).get(this.c));
    }

    public ClientSettingsFrameSectionOpenClickHandler(ClientSettingsFrame sG2, String string) {
        this.I = sG2;
        this.c = string;
    }
}
