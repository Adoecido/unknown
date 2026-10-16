package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrame;

public class ClientSettingsSearchFrameClassOpenClickHandler
implements GuiClickListener {
    private final Class F;
    final ClientSettingsSearchFrame K;

    public Class p() {
        return this.F;
    }

    public ClientSettingsSearchFrameClassOpenClickHandler(ClientSettingsSearchFrame clientSettingsSearchFrame, Class clazz) {
        this.K = clientSettingsSearchFrame;
        this.F = clazz;
    }

    @Override
    public void onPrimaryClick() {
        ClientSettings.showFrame(this.F);
    }
}
