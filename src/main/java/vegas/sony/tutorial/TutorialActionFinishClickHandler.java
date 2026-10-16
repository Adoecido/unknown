package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.TutorialPage;
import vegas.sony.ui.click.component.GuiClickListener;

class TutorialActionFinishClickHandler
implements GuiClickListener {
    final TutorialPage w;

    TutorialActionFinishClickHandler(TutorialPage tutorialPage) {
        this.w = tutorialPage;
    }

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getTutorialManager().completeCurrentPage();
    }
}
