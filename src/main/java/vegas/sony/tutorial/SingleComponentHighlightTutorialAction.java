package vegas.sony.tutorial;

import func.skidline.RectData;
import vegas.sony.SonyVegas;
import vegas.sony.tutorial.HighlightTutorialAction;
import vegas.sony.tutorial.SingleComponentHighlightAdvanceClickListener;
import vegas.sony.tutorial.TutorialActionComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;

public class SingleComponentHighlightTutorialAction
extends HighlightTutorialAction {
    private static final EncString g = Str.enc("start");


    public SingleComponentHighlightTutorialAction(GuiComponent guiComponent, String string, String string2, boolean bl) {
        super(new TutorialActionComponent(string, string2), bl);
        this.getHighlightedComponents().add(guiComponent);
    }

    public GuiComponent getHighlightedComponent() {
        return this.getHighlightedComponents().size() > 0 ? this.getHighlightedComponents().get(0) : null;
    }

    @Override
    public RectData getHighlightBounds() {
        GuiComponent guiComponent = this.getHighlightedComponent();
        guiComponent.c();
        return new RectData(guiComponent.G$src$D$1b2f02a(), guiComponent.n(), guiComponent.A(), guiComponent.L());
    }

    @Override
    public void start() {
        SonyVegas.debugLog(g.get() + this);
        if (this.includesHiddenComponents()) {
            SingleComponentHighlightAdvanceClickListener singleComponentHighlightAdvanceClickListener = new SingleComponentHighlightAdvanceClickListener(this);
            this.registerMouseListener(this.getHighlightedComponent(), singleComponentHighlightAdvanceClickListener);
            this.getComponent().getActionButton().setVisible(false);
        }
    }

    @Override
    public boolean isTargetReady() {
        return this.getHighlightedComponent().V$src$Z$1xhop3l();
    }
}
