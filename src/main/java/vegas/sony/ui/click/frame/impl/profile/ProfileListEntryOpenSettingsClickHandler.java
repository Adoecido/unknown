package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.profile.ProfileListEntryComponent;

class ProfileListEntryOpenSettingsClickHandler
implements GuiClickListener {
    private final ProfileListEntryComponent entry;
    private final Profile profile;

    ProfileListEntryOpenSettingsClickHandler(ProfileListEntryComponent entry, Profile profile) {
        this.entry = entry;
        this.profile = profile;
    }

    @Override
    public void onPrimaryClick() {
        if (SonyVegas.INSTANCE.getProfilesManager().getActiveProfile().equals(this.profile)) {
            this.profile.captureCurrentState();
        }
        this.entry.openSettings();
    }

}

