package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.TutorialFinishedPanel;
import vegas.sony.tutorial.TutorialState;
import vegas.sony.ui.click.component.GuiClickListener;

public class TutorialFinishedAcknowledgeClickHandler
implements GuiClickListener {
    final TutorialFinishedPanel Z;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getTutorialManager().setState(TutorialState.FINISHED);
    }

    public TutorialFinishedAcknowledgeClickHandler(TutorialFinishedPanel tutorialFinishedPanel) {
        this.Z = tutorialFinishedPanel;
    }
}
