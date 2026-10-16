package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.MultiComponentHighlightTutorialAction;
import vegas.sony.tutorial.TutorialPage;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

class MultiComponentHighlightAdvanceClickListener
implements GuiMouseListener {
    final MultiComponentHighlightTutorialAction o;

    @Override
    public void I(Point point) {
    }


    MultiComponentHighlightAdvanceClickListener(MultiComponentHighlightTutorialAction multiComponentHighlightTutorialAction) {
        this.o = multiComponentHighlightTutorialAction;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (mouseClickButton != MouseClickButton.LEFT_CLICK) {
            return;
        }
        TutorialPage tutorialPage = this.o.getPage();
        if (tutorialPage.getCurrentAction() != null && tutorialPage.getCurrentAction().equals(this.o)) {
            SonyVegas.INSTANCE.getTutorialManager().getCurrentPage().advanceToNextAction();
        }
    }
}

