package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.TextGuiTutorialPage;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.ClickGuiQuickActionsComponent;
import java.util.ArrayList;
import java.util.Arrays;

public class TextGuiQuickActionsSettingsButtonTargetSelector
extends TutorialTargetSelector<ClickGuiQuickActionsComponent> {
    private final TextGuiTutorialPage tutorialPage;
    private ClickGuiQuickActionsComponent selectedQuickActions;

    private boolean matchesQuickActions(ClickGuiQuickActionsComponent clickGuiQuickActionsComponent) {
        this.selectedQuickActions = clickGuiQuickActionsComponent;
        return true;
    }

    @Override
    public boolean matches(ClickGuiQuickActionsComponent clickGuiQuickActionsComponent) {
        return this.matchesQuickActions(clickGuiQuickActionsComponent);
    }

    public TextGuiQuickActionsSettingsButtonTargetSelector(TextGuiTutorialPage textGuiTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = textGuiTutorialPage;
        this.selectedQuickActions = null;
    }

    @Override
    public ArrayList<GuiComponent> findTargets(GuiComponent guiComponent) {
        if (this.getTargetType().isInstance(guiComponent)) {
            ArrayList<GuiComponent> arrayList = super.findTargets(guiComponent);
            if (arrayList != null && this.selectedQuickActions != null && this.selectedQuickActions.equals(guiComponent)) {
                return new ArrayList<GuiComponent>(Arrays.asList(this.selectedQuickActions.b$src$Lvegas_sony_ui_click_component_IconButtonComponent_$1sg98rj()));
            }
            return arrayList;
        }
        return null;
    }

}
