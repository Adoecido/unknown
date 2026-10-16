package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.TutorialWelcomePanel;
import vegas.sony.ui.click.component.GuiClickListener;

class TutorialWelcomeStartClickHandler
implements GuiClickListener {
    final TutorialWelcomePanel F;

    TutorialWelcomeStartClickHandler(TutorialWelcomePanel tutorialWelcomePanel) {
        this.F = tutorialWelcomePanel;
    }

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getTutorialManager().startTutorial();
    }
}
