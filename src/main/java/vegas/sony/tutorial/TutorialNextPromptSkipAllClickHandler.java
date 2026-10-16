package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.TutorialNextPromptPanel;
import vegas.sony.tutorial.TutorialState;
import vegas.sony.ui.click.component.GuiClickListener;

public class TutorialNextPromptSkipAllClickHandler
implements GuiClickListener {
    final TutorialNextPromptPanel T;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getTutorialManager().setState(TutorialState.COMPLETED_ALL);
    }

    public TutorialNextPromptSkipAllClickHandler(TutorialNextPromptPanel tutorialNextPromptPanel) {
        this.T = tutorialNextPromptPanel;
    }
}
