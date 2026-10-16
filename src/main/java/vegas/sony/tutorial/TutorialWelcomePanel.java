package vegas.sony.tutorial;

import vegas.sony.tutorial.TutorialOverlayPanelBase;
import vegas.sony.tutorial.TutorialWelcomeSkipAllClickHandler;
import vegas.sony.tutorial.TutorialWelcomeStartButton;
import vegas.sony.tutorial.TutorialWelcomeStartClickHandler;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.IconGlyphComponent;
import vegas.sony.ui.click.component.WrappingTextLabelComponent;
import vegas.sony.ui.click.component.gui.UnderlinedTextLabel;
import vegas.sony.utils.render.GuiRenderPrimitives;
import java.awt.Color;

public class TutorialWelcomePanel
extends TutorialOverlayPanelBase {
    private static int obfuscationState;

    static {
        TutorialWelcomePanel.setObfuscationState(88);
    }

    public static void setObfuscationState(int state) {
        obfuscationState = state;
    }

    public static int getObfuscationState() {
        return obfuscationState;
    }

    @Override
    public void H() {
        this.setDisabledOverlayColor(new Color(26, 26, 26, 255));
        GuiRenderPrimitives.P(this.G$src$D$1b2f02a() - 1.0, this.n() - 1.0, this.A() + 2.0, this.L() + 2.0 + 2.0, new Color(38, 38, 38, 255), 1.0f, 2.0f, 1.0f);
        super.H();
    }

    @Override
    public void c() {
        super.c();
    }

    @Override
    public double C() {
        return 140.0;
    }

    public TutorialWelcomePanel() {
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().t(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().I(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().U(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().u(false);
        this.h(new IconGlyphComponent("mask_l_1", 32.0f, 38.0f), "topLeft");
        this.h(new IconGlyphComponent("mask_r_1", 32.0f, 46.0f), "bottomRight");
        this.h(new WrappingTextLabelComponent("Welcome", 1.25, TutorialWelcomePanel.J.Z), "spanwidth, offsetY 20, offsetX 10");
        this.h(new WrappingTextLabelComponent("Welcome!\n \nIf you are new, please proceed with the tutorial", 0.8, TutorialWelcomePanel.J.Z), "spanwidth, offsetY 30, offsetX 20");
        UnderlinedTextLabel underlinedTextLabel = new UnderlinedTextLabel("Skip tutorial", 0.85, TutorialWelcomePanel.J.h);
        this.h(underlinedTextLabel, "offsetX 30, offsetY 80");
        TutorialWelcomeStartButton tutorialWelcomeStartButton = new TutorialWelcomeStartButton(this, "Tutorial", TutorialWelcomePanel.J.U, TutorialWelcomePanel.J.U.brighter());
        ((GuiComponent)tutorialWelcomeStartButton).o(44.0);
        tutorialWelcomeStartButton.Y(14.0);
        this.h(tutorialWelcomeStartButton, "offsetX 70, offsetY 77");
        underlinedTextLabel.setClickListener(new TutorialWelcomeSkipAllClickHandler(this));
        tutorialWelcomeStartButton.setClickListener(new TutorialWelcomeStartClickHandler(this));
    }

    @Override
    public double x() {
        return 150.0;
    }


    public static int P$src$I$x6acxy() {
        int n = TutorialWelcomePanel.getObfuscationState();
        return 0;
    }
}

