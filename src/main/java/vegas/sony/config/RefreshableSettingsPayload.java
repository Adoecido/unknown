package vegas.sony.config;

import vegas.sony.config.SettingsPayload;

public interface RefreshableSettingsPayload
extends SettingsPayload {
    @Override
    public void initializeDefaults();

    default public void refreshFromCurrentSettings() {
    }
}
