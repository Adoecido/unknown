package vegas.sony.tutorial.selector;

import vegas.sony.tutorial.TutorialTargetSelector;
import vegas.sony.tutorial.page.TextGuiTutorialPage;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.SettingsFrameHeaderComponent;

public class TextGuiSettingsNonHeaderComponentTargetSelector
extends TutorialTargetSelector<GuiComponent> {
    private final TextGuiTutorialPage tutorialPage;

    public TextGuiSettingsNonHeaderComponentTargetSelector(TextGuiTutorialPage textGuiTutorialPage, Class clazz) {
        super(clazz);
        this.tutorialPage = textGuiTutorialPage;
    }

    @Override
    public boolean matches(GuiComponent guiComponent) {
        boolean bl = !(guiComponent instanceof SettingsFrameHeaderComponent);
        return bl;
    }

}

