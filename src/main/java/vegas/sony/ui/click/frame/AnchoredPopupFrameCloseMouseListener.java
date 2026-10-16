package vegas.sony.ui.click.frame;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.AnchoredPopupFrame;
import java.awt.Point;

class AnchoredPopupFrameCloseMouseListener
implements GuiMouseListener {
    final AnchoredPopupFrame O;


    AnchoredPopupFrameCloseMouseListener(AnchoredPopupFrame anchoredPopupFrame) {
        this.O = anchoredPopupFrame;
    }

    @Override
    public void g(Point point, MouseClickButton uA) {
        ClientSettings.removePopup(this.O);
        Runnable runnable = AnchoredPopupFrame.b(this.O);
        if (runnable != null) {
            runnable.run();
        }
    }
}

