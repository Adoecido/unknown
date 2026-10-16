package vegas.sony.ui.click.frame;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.AnchoredPopupFrame;
import java.awt.Point;

class AnchoredPopupFrameOutsideCloseMouseListener
implements GuiMouseListener {
    final GuiComponent J;
    final AnchoredPopupFrame q;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (!this.q.w$src$Z$e457mb() && !this.J.w$src$Z$e457mb()) {
            ClientSettings.removePopup(this.q);
            Runnable runnable = AnchoredPopupFrame.b(this.q);
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    AnchoredPopupFrameOutsideCloseMouseListener(AnchoredPopupFrame anchoredPopupFrame, GuiComponent guiComponent) {
        this.q = anchoredPopupFrame;
        this.J = guiComponent;
    }

}

