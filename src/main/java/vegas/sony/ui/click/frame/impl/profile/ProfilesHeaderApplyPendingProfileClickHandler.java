package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.profile.ProfilesSettingsFrame;
import vegas.sony.ui.click.frame.impl.profile.ProfilesSettingsHeaderComponent;

class ProfilesHeaderApplyPendingProfileClickHandler
implements GuiClickListener {
    private final ProfilesSettingsFrame profilesFrame;

    ProfilesHeaderApplyPendingProfileClickHandler(ProfilesSettingsFrame profilesFrame) {
        this.profilesFrame = profilesFrame;
    }

    @Override
    public void onPrimaryClick() {
        this.profilesFrame.closePopupAndDiscardDraft();
    }
}
