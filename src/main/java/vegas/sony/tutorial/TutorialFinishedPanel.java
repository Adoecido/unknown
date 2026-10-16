package vegas.sony.tutorial;

import vegas.sony.tutorial.TutorialFinishedAcknowledgeClickHandler;
import vegas.sony.tutorial.TutorialOverlayPanelBase;
import vegas.sony.ui.click.component.gui.TextButton;
import vegas.sony.ui.click.component.gui.WrappedTextComponent;
import vegas.sony.utils.render.GuiRenderPrimitives;
import java.awt.Color;

public class TutorialFinishedPanel
extends TutorialOverlayPanelBase {
    @Override
    public double x() {
        return 140.0;
    }

    public TutorialFinishedPanel() {
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().t(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().I(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().U(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().u(false);
        this.h(new WrappedTextComponent("All tutorials have been completed!", 1.25, TutorialFinishedPanel.J.Z, true), "spanwidth, offsetY 15, offsetX 15");
        this.h(new WrappedTextComponent("If you have any further questions, please contact support", 0.8, TutorialFinishedPanel.J.Z, false), "spanwidth, offsetY 45, offsetX 15");
        TextButton textButton = new TextButton("End Tutorial", TutorialFinishedPanel.J.U, TutorialFinishedPanel.J.U.brighter());
        textButton.o(44.0);
        textButton.Y(14.0);
        this.h(textButton, "spanwidth, offsetX 15, offsetY 70");
        textButton.setClickListener(new TutorialFinishedAcknowledgeClickHandler(this));
    }

    @Override
    public void H() {
        this.setDisabledOverlayColor(new Color(26, 26, 26, 255));
        GuiRenderPrimitives.P(this.G$src$D$1b2f02a() - 1.0, this.n() - 1.0, this.A() + 2.0, this.L() + 2.0 + 2.0, new Color(38, 38, 38, 255), 1.0f, 2.0f, 1.0f);
        super.H();
    }

    @Override
    public double C() {
        return 95.0;
    }

    @Override
    public void c() {
        super.c();
    }
}
