package vegas.sony.tutorial;

import vegas.sony.tutorial.TutorialPage;
import vegas.sony.ui.click.component.GuiClickListener;

class TutorialActionNextClickHandler
implements GuiClickListener {
    final TutorialPage o;

    @Override
    public void onPrimaryClick() {
        this.o.advanceToNextAction();
    }

    TutorialActionNextClickHandler(TutorialPage bF) {
        this.o = bF;
    }
}
