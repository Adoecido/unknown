package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.TextGuiTutorialPage;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.SettingsFrameHeaderComponent;
import java.util.ArrayList;
import java.util.Arrays;

public class SettingsFrameHeaderSecondaryButtonTargetSelector
extends TutorialTargetSelector<SettingsFrameHeaderComponent> {
    private final TextGuiTutorialPage tutorialPage;
    private SettingsFrameHeaderComponent selectedHeader;

    private boolean matchesHeader(SettingsFrameHeaderComponent settingsFrameHeaderComponent) {
        this.selectedHeader = settingsFrameHeaderComponent;
        return true;
    }

    @Override
    public boolean matches(SettingsFrameHeaderComponent settingsFrameHeaderComponent) {
        return this.matchesHeader(settingsFrameHeaderComponent);
    }

    @Override
    public ArrayList<GuiComponent> findTargets(GuiComponent guiComponent) {
        if (this.getTargetType().isInstance(guiComponent)) {
            ArrayList<GuiComponent> arrayList = super.findTargets(guiComponent);
            if (arrayList != null && this.selectedHeader != null && this.selectedHeader.equals(guiComponent)) {
                return new ArrayList<GuiComponent>(Arrays.asList(this.selectedHeader.x$src$Lvegas_sony_ui_click_component_IconButtonComponent_$x1h5th()));
            }
            return arrayList;
        }
        return null;
    }


    public SettingsFrameHeaderSecondaryButtonTargetSelector(TextGuiTutorialPage textGuiTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = textGuiTutorialPage;
        this.selectedHeader = null;
    }
}
