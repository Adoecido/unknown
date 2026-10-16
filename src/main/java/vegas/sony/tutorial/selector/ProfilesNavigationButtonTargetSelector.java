package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.ProfilesTutorialPage;
import vegas.sony.ui.click.frame.FrameNavigationButtonComponent;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;

public class ProfilesNavigationButtonTargetSelector
extends TutorialTargetSelector<FrameNavigationButtonComponent> {
    private static final EncString targetPageName = Str.enc("Profiles");
    private final ProfilesTutorialPage tutorialPage;

    private boolean matchesPage(FrameNavigationButtonComponent frameNavigationButtonComponent) {
        return frameNavigationButtonComponent.N$src$Ljava_lang_String_$wy122q().equals(targetPageName.get());
    }

    @Override
    public boolean matches(FrameNavigationButtonComponent frameNavigationButtonComponent) {
        return this.matchesPage(frameNavigationButtonComponent);
    }

    public ProfilesNavigationButtonTargetSelector(ProfilesTutorialPage profilesTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = profilesTutorialPage;
    }
}
