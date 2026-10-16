package vegas.sony.ui.click.frame.impl.target;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.impl.hud.HudModuleFrameBase;
import vegas.sony.utils.render.GuiRenderPrimitives;
import java.awt.Color;

public class TargetInfoStatStripComponent
extends GuiComponent {
    protected HudModuleFrameBase frame;

    public TargetInfoStatStripComponent(int n, int n2) {
        this.o(n);
        this.Y(n2);
        this.setDisabledOverlayColor(new Color(255, 255, 255, 18));
    }

    @Override
    public void H() {
        Color color = this.frame != null ? this.frame.applyDefaultEditorAlpha(this.getDisabledOverlayColor()) : this.getDisabledOverlayColor();
        GuiRenderPrimitives.e(this.G$src$D$1b2f02a() + 1.0, this.n(),
                this.A() - 2.0, this.L(), color, false, 3.0f, 1.0f);
    }

    public void setFrame(HudModuleFrameBase frame) {
        this.frame = frame;
    }
}

