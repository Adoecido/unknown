package vegas.sony.config;

import vegas.sony.config.PublicProfileSettings;
import vegas.sony.input.BindSet;

public class PublicProfileSettingsBindSet
extends BindSet {
    final PublicProfileSettings publicProfileSettings;

    @Override
    public void onBindActivated() {
    }

    public PublicProfileSettingsBindSet(PublicProfileSettings publicProfileSettings, int keyCode) {
        super(keyCode);
        this.publicProfileSettings = publicProfileSettings;
    }
}
