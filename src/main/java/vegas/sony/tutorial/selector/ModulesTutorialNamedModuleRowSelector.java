package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.ModulesTutorialPage;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;

public class ModulesTutorialNamedModuleRowSelector
extends TutorialTargetSelector<ModuleComponent> {
    private final ModulesTutorialPage tutorialPage;
    private static final EncString targetModuleName = Str.enc("Clicker");

    private boolean matchesModule(ModuleComponent moduleComponent) {
        return moduleComponent.getModule().getName().equals(targetModuleName.get());
    }

    @Override
    public boolean matches(ModuleComponent moduleComponent) {
        return this.matchesModule(moduleComponent);
    }

    public ModulesTutorialNamedModuleRowSelector(ModulesTutorialPage modulesTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = modulesTutorialPage;
    }
}
