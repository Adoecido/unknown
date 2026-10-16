package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.HalfScaleIconButtonComponent;
import vegas.sony.ui.click.component.IconButtonComponent;
import vegas.sony.ui.click.frame.FrameHeaderComponent;
import vegas.sony.ui.click.frame.FrameHeaderMainLayerClickHandler;
import vegas.sony.ui.click.frame.impl.hud.HudEditorReturnToMainLayerFrame;

public class HudEditorReturnToMainLayerHeaderComponent
extends FrameHeaderComponent {
    private static final String ICON = "cheat_switch";
    private final HudEditorReturnToMainLayerFrame frame;
    private final IconButtonComponent returnButton = new HalfScaleIconButtonComponent(ICON);

    @Override
    public boolean V$src$Z$1xhop3l() {
        return true;
    }

    @Override
    public void I() {
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
    }

    @Override
    public void H() {
        this.frame.centerAtTop();
        this.returnButton.K(this.G$src$D$1b2f02a() + 1.0);
        this.returnButton.S(this.n() + 1.5);
        this.returnButton.o(this.A());
        this.returnButton.Y(this.L());
        if (this.w$src$Z$e457mb()) {
            this.returnButton.setOverrideColor(J.z().brighter());
        } else {
            this.returnButton.setOverrideColor(J.z());
        }
        this.returnButton.setVisible(true);
    }

    @Override
    public double A() {
        return 22.0;
    }

    @Override
    public void F() {
    }

    public IconButtonComponent getReturnButton() {
        return this.returnButton;
    }

    @Override
    public double L() {
        return 16.0;
    }

    @Override
    public void u() {
    }

    public HudEditorReturnToMainLayerHeaderComponent(HudEditorReturnToMainLayerFrame hudEditorReturnToMainLayerFrame) {
        super(hudEditorReturnToMainLayerFrame);
        this.frame = hudEditorReturnToMainLayerFrame;
        this.returnButton.addClickListener(new FrameHeaderMainLayerClickHandler());
        this.addChildren(this.returnButton);
        this.setVisible(true);
    }
}

