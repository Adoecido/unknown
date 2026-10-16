package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.SingleComponentHighlightTutorialAction;
import vegas.sony.tutorial.TutorialPage;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

public class SingleComponentHighlightAdvanceClickListener
implements GuiMouseListener {
    final SingleComponentHighlightTutorialAction H;

    @Override
    public void I(Point point) {
    }


    public SingleComponentHighlightAdvanceClickListener(SingleComponentHighlightTutorialAction singleComponentHighlightTutorialAction) {
        this.H = singleComponentHighlightTutorialAction;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (mouseClickButton != MouseClickButton.LEFT_CLICK) {
            return;
        }
        TutorialPage tutorialPage = this.H.getPage();
        if (tutorialPage.getCurrentAction() != null && tutorialPage.getCurrentAction().equals(this.H)) {
            SonyVegas.INSTANCE.getTutorialManager().getCurrentPage().advanceToNextAction();
        }
    }
}

