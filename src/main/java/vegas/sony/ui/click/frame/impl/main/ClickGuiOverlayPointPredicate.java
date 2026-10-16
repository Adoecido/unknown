package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.frame.impl.main.ClickGuiMainFrame;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayController;
import java.awt.Point;

class ClickGuiOverlayPointPredicate
implements GuiMouseListener {
    final ClickGuiMainFrame frame;
    final ClickGuiOverlayController controller;

    ClickGuiOverlayPointPredicate(ClickGuiOverlayController clickGuiOverlayController, ClickGuiMainFrame clickGuiMainFrame) {
        this.controller = clickGuiOverlayController;
        this.frame = clickGuiMainFrame;
    }

    @Override
    public boolean Q(Point point) {
        return ClickGuiOverlayController.getBackdrop(this.controller).getBounds().R(point);
    }
}
