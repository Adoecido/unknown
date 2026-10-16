package vegas.sony.manager.client;

import vegas.sony.manager.client.OnlineSettings;
import vegas.sony.unmap.Bendable;

class OnlineSettingsNoopBindAction
extends Bendable {
    final OnlineSettings settings;

    OnlineSettingsNoopBindAction(OnlineSettings onlineSettings) {
        this.settings = onlineSettings;
    }

    @Override
    public void onBindActivated() {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public String getDisplayText() {
        return null;
    }
}
