package vegas.sony.ui.click.component;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.DropdownSelectComponent;
import vegas.sony.ui.click.frame.PopupFrame;
import java.awt.Point;

public class DropdownPopupCloseClickHandler
implements GuiMouseListener {
    final DropdownSelectComponent<?> owner;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        PopupFrame popupFrame = this.owner.getPopupFrame();
        if (popupFrame != null && !this.owner.t() && !popupFrame.t()) {
            this.owner.togglePopup();
        }
    }


    public DropdownPopupCloseClickHandler(DropdownSelectComponent<?> dropdown) {
        this.owner = dropdown;
    }
}

