package vegas.sony.tutorial.page.profile;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.ProfilesTutorialPage;
import vegas.sony.ui.click.frame.impl.profile.ProfileListEntryComponent;

public class FirstActiveProfileListEntrySelector
extends TutorialTargetSelector<ProfileListEntryComponent> {
    private final ProfilesTutorialPage tutorialPage;


    public FirstActiveProfileListEntrySelector(ProfilesTutorialPage profilesTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = profilesTutorialPage;
    }

    private boolean matchesActiveProfile(ProfileListEntryComponent profileListEntryComponent) {
        return profileListEntryComponent.isActiveProfile();
    }

    @Override
    public boolean matches(ProfileListEntryComponent profileListEntryComponent) {
        return this.matchesActiveProfile(profileListEntryComponent);
    }
}
