package vegas.sony.tutorial;

import vegas.sony.SonyVegas;
import vegas.sony.tutorial.TutorialAction;
import vegas.sony.tutorial.TutorialActionFinishClickHandler;
import vegas.sony.tutorial.TutorialActionNextClickHandler;
import vegas.sony.tutorial.TutorialFrame;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import java.util.ArrayList;
import java.util.List;

public class TutorialPage {
    private final List<TutorialAction> actions = new ArrayList<TutorialAction>();
    private TutorialAction currentAction;
    private final EncString title;
    private TutorialFrame tutorialFrame;
    private static final String debugPrefix;
    private static GuiComponent[] obfuscationState;

    public TutorialAction getCurrentAction() {
        return this.currentAction;
    }

    public void addAction(TutorialAction tutorialAction) {
        this.actions.add(tutorialAction);
        tutorialAction.getComponent().getActionButton().setClickListener(new TutorialActionNextClickHandler(this));
        tutorialAction.getComponent().getSkipLabel().setClickListener(new TutorialActionFinishClickHandler(this));
        tutorialAction.setPage(this);
    }

    public TutorialPage(String title) {
        this.title = Str.enc(title);
    }

    public void resetAndStart() {
        this.currentAction = null;
        this.advanceToNextAction();
    }

    public String getTitle() {
        return this.title == null ? null : this.title.get();
    }

    public static GuiComponent[] getObfuscationState() {
        return obfuscationState;
    }


    static {
        TutorialPage.setObfuscationState(new GuiComponent[2]);
        debugPrefix = "3 ";
    }

    public void activateAction(TutorialAction tutorialAction) {
        if (this.currentAction != null) {
            this.currentAction.cleanup();
        }
        this.currentAction = tutorialAction;
        this.currentAction.start();
        if (this.tutorialFrame != null) {
            this.tutorialFrame.t$src$V$zbu1jn();
            this.tutorialFrame.h(tutorialAction.getComponent(), new Object[0]);
            this.tutorialFrame.l$src$V$1mibm4x();
        }
    }

    public static void setObfuscationState(GuiComponent[] guiComponentArray) {
        obfuscationState = guiComponentArray;
    }

    public void advanceToNextAction() {
        SonyVegas.debugLog("1");
        if (this.currentAction != null) {
            if (!this.currentAction.canAdvance()) {
                return;
            }
            boolean bl = false;
            boolean bl2 = false;
            SonyVegas.debugLog("2");
            for (TutorialAction tutorialAction : this.actions) {
                if (bl) {
                    this.activateAction(tutorialAction);
                    bl2 = true;
                    break;
                }
                if (!tutorialAction.equals(this.currentAction)) continue;
                bl = true;
            }
            SonyVegas.debugLog(debugPrefix + bl + " " + bl2);
            if (!bl2) {
                SonyVegas.INSTANCE.getTutorialManager().completeCurrentPage();
            }
        } else {
            this.activateAction(this.actions.get(0));
        }
    }

    public void setFrame(TutorialFrame tutorialFrame) {
        this.tutorialFrame = tutorialFrame;
    }
}

