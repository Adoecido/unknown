package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.TutorialState;
import vegas.sony.tutorial.TutorialWelcomePanel;
import vegas.sony.ui.click.component.GuiClickListener;

class TutorialWelcomeSkipAllClickHandler
implements GuiClickListener {
    final TutorialWelcomePanel F;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getTutorialManager().setState(TutorialState.COMPLETED_ALL);
    }

    TutorialWelcomeSkipAllClickHandler(TutorialWelcomePanel tutorialWelcomePanel) {
        this.F = tutorialWelcomePanel;
    }
}
