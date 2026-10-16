package vegas.sony.config;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.unmap.BindChangeListener;

class ClientSettingsBindChangeListener
implements BindChangeListener {
    final ClientSettings clientSettings;

    ClientSettingsBindChangeListener(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }

    @Override
    public void onBindChanged() {
        SonyVegas.INSTANCE.getFriendManager().toggleCrosshairTarget();
    }
}
