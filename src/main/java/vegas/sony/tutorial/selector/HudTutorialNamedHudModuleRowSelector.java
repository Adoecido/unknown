package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.HudTutorialPage;
import vegas.sony.ui.click.frame.impl.hud.HudModuleListEntry;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;

public class HudTutorialNamedHudModuleRowSelector
extends TutorialTargetSelector<HudModuleListEntry> {
    private final HudTutorialPage tutorialPage;
    private static final EncString targetModuleName = Str.enc("Freelook");

    public HudTutorialNamedHudModuleRowSelector(HudTutorialPage hudTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = hudTutorialPage;
    }

    private boolean matchesTargetName(HudModuleListEntry hudModuleListEntry) {
        return hudModuleListEntry.getModule().getName().equals(targetModuleName.get());
    }

    @Override
    public boolean matches(HudModuleListEntry hudModuleListEntry) {
        return this.matchesTargetName(hudModuleListEntry);
    }
}
