package vegas.sony.ui.click.component.gui;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.PopupMenuButtonComponent;
import java.awt.Point;

public class InteractivePopupOutsideCloseMouseListener
implements GuiMouseListener {
    final PopupMenuButtonComponent owner;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (!this.owner.t() && !this.owner.getPopupFrame().t()) {
            this.owner.togglePopup();
        }
    }


    public InteractivePopupOutsideCloseMouseListener(PopupMenuButtonComponent popupMenuButtonComponent) {
        this.owner = popupMenuButtonComponent;
    }
}

