package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.ModulesTutorialPage;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import java.util.ArrayList;
import java.util.Arrays;

public class ModulesTutorialValuesExpandButtonSelector
extends TutorialTargetSelector<ModuleComponent> {
    private ModuleComponent selectedModule;
    private final ModulesTutorialPage tutorialPage;
    private static final EncString targetModuleName = Str.enc("Clicker");

    @Override
    public ArrayList<GuiComponent> findTargets(GuiComponent guiComponent) {
        if (this.getTargetType().isInstance(guiComponent)) {
            ArrayList<GuiComponent> arrayList = super.findTargets(guiComponent);
            if (arrayList != null && this.selectedModule != null && this.selectedModule.equals(guiComponent)) {
                return new ArrayList<GuiComponent>(Arrays.asList(this.selectedModule.getSettingsButton()));
            }
            return arrayList;
        }
        return null;
    }

    public ModulesTutorialValuesExpandButtonSelector(ModulesTutorialPage modulesTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = modulesTutorialPage;
        this.selectedModule = null;
    }


    private boolean matchesModule(ModuleComponent moduleComponent) {
        if (moduleComponent.getModule().getName().equals(targetModuleName.get())) {
            this.selectedModule = moduleComponent;
            return true;
        }
        return false;
    }

    @Override
    public boolean matches(ModuleComponent moduleComponent) {
        return this.matchesModule(moduleComponent);
    }
}
