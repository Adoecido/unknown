package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.ModulesTutorialPage;
import vegas.sony.ui.click.frame.ModuleCategoryNavigationButtonComponent;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;

public class ModulesTutorialCategoryButtonSelector
extends TutorialTargetSelector<ModuleCategoryNavigationButtonComponent> {
    private static final EncString targetCategoryName = Str.enc("Combat");
    private final ModulesTutorialPage tutorialPage;

    private boolean matchesCategory(ModuleCategoryNavigationButtonComponent moduleCategoryNavigationButtonComponent) {
        return moduleCategoryNavigationButtonComponent.N$src$Ljava_lang_String_$wy122q().equals(targetCategoryName.get());
    }

    @Override
    public boolean matches(ModuleCategoryNavigationButtonComponent moduleCategoryNavigationButtonComponent) {
        return this.matchesCategory(moduleCategoryNavigationButtonComponent);
    }

    public ModulesTutorialCategoryButtonSelector(ModulesTutorialPage modulesTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = modulesTutorialPage;
    }
}
