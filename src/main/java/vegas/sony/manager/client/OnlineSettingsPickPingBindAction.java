package vegas.sony.manager.client;

import vegas.sony.friend.ping.PingManager;
import vegas.sony.manager.client.OnlineSettings;
import vegas.sony.unmap.Bendable;

class OnlineSettingsPickPingBindAction
extends Bendable {
    final OnlineSettings settings;

    OnlineSettingsPickPingBindAction(OnlineSettings onlineSettings) {
        this.settings = onlineSettings;
    }

    @Override
    public String getDisplayText() {
        return null;
    }

    @Override
    public void onBindActivated() {
        PingManager.INSTANCE.onEnable();
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
