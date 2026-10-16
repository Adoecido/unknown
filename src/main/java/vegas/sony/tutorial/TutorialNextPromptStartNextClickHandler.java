package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.TutorialNextPromptPanel;
import vegas.sony.ui.click.component.GuiClickListener;

public class TutorialNextPromptStartNextClickHandler
implements GuiClickListener {
    final TutorialNextPromptPanel V;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getTutorialManager().startNextPage();
    }

    public TutorialNextPromptStartNextClickHandler(TutorialNextPromptPanel tutorialNextPromptPanel) {
        this.V = tutorialNextPromptPanel;
    }
}
