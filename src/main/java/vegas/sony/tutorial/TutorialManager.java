package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.tutorial.TutorialFinishedPanel;
import vegas.sony.tutorial.TutorialFrame;
import vegas.sony.tutorial.TutorialNextPromptPanel;
import vegas.sony.tutorial.TutorialPage;
import vegas.sony.tutorial.TutorialState;
import vegas.sony.tutorial.TutorialWelcomePanel;
import vegas.sony.tutorial.page.ModulesTutorialPage;
import vegas.sony.tutorial.page.ProfilesTutorialPage;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import java.util.ArrayList;
import java.util.List;

public class TutorialManager {
    private final TutorialFrame tutorialFrame;
    private final List<TutorialPage> pages = new ArrayList<TutorialPage>();
    private static final EncString allCompleteMessage = Str.enc("all complete");
    private TutorialState state = null;
    private TutorialPage currentPage;

    public void showWelcomeScreen() {
        this.tutorialFrame.t$src$V$zbu1jn();
        this.tutorialFrame.addChildren(new TutorialWelcomePanel());
        this.tutorialFrame.l$src$V$1mibm4x();
    }

    public void setState(TutorialState tutorialState) {
        if (this.state != null && this.state.equals((Object)TutorialState.FINISHED) && !tutorialState.equals((Object)TutorialState.INDEX) && !tutorialState.equals((Object)TutorialState.WELCOME)) {
            return;
        }
        this.state = tutorialState;
        switch (tutorialState) {
            case WELCOME: {
                this.showWelcomeScreen();
                break;
            }
            case COMPLETED_ALL: {
                this.showFinishedScreen();
                break;
            }
            case COMPLETED_TUTORIAL: {
                this.showNextPagePrompt();
            }
        }
    }

    public void refreshCurrentAction() {
        if (this.getCurrentPage() != null && this.state == TutorialState.IN_TUTORIAL) {
            this.getCurrentPage().getCurrentAction().render();
        }
    }

    public void startTutorial() {
        this.openPage(this.pages.get(0));
    }

    public void startNextPage() {
        this.openPage(this.getNextPage());
    }

    public void showNextPagePrompt() {
        this.tutorialFrame.t$src$V$zbu1jn();
        this.tutorialFrame.addChildren(new TutorialNextPromptPanel(this.currentPage.getTitle(), this.getNextPage().getTitle()));
        this.tutorialFrame.l$src$V$1mibm4x();
    }

    public void openPage(TutorialPage tutorialPage) {
        this.setState(TutorialState.IN_TUTORIAL);
        this.currentPage = tutorialPage;
        this.tutorialFrame.t$src$V$zbu1jn();
        this.tutorialFrame.l$src$V$1mibm4x();
        tutorialPage.resetAndStart();
    }

    public TutorialPage getNextPage() {
        int n = 0;
        for (int i = 0; i < this.pages.size(); ++i) {
            TutorialPage tutorialPage = this.pages.get(i);
            if (!tutorialPage.equals(this.currentPage)) continue;
            n = i;
        }
        if (n + 1 >= this.pages.size()) {
            return null;
        }
        return this.pages.get(n + 1);
    }

    public void updateVisibility() {
        boolean shouldShow = this.shouldShowFrame();
        if (shouldShow != this.tutorialFrame.V$src$Z$1xhop3l()) {
            this.tutorialFrame.setVisible(shouldShow);
        }
    }

    public TutorialPage getCurrentPage() {
        return this.currentPage;
    }

    public void completeCurrentPage() {
        if (this.getNextPage() == null) {
            SonyVegas.debugLog(allCompleteMessage.get());
            this.setState(TutorialState.COMPLETED_ALL);
        } else {
            this.setState(TutorialState.COMPLETED_TUTORIAL);
        }
    }

    private boolean shouldShowFrame() {
        if (this.state == TutorialState.IN_TUTORIAL && this.currentPage != null) {
            return this.currentPage.getCurrentAction() != null && this.currentPage.getCurrentAction().isTargetReady();
        }
        return this.state != TutorialState.FINISHED;
    }

    public void showFinishedScreen() {
        this.tutorialFrame.t$src$V$zbu1jn();
        this.tutorialFrame.addChildren(new TutorialFinishedPanel());
        this.tutorialFrame.l$src$V$1mibm4x();
    }


    private void registerPage(TutorialPage tutorialPage) {
        this.pages.add(tutorialPage);
        tutorialPage.setFrame(this.tutorialFrame);
    }

    public TutorialManager() {
        this.tutorialFrame = ClientSettings.getFrame(TutorialFrame.class);
        this.registerPage(new ModulesTutorialPage());
        this.registerPage(new ProfilesTutorialPage());
    }
}

