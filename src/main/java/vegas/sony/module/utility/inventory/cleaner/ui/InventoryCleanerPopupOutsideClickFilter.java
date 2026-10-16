package vegas.sony.module.utility.inventory.cleaner.ui;

import vegas.sony.module.utility.inventory.cleaner.ui.InventoryCleanerPopupFrame;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.frame.PopupFrame;
import java.awt.Point;

class InventoryCleanerPopupOutsideClickFilter
implements GuiMouseListener {
    final InventoryCleanerPopupFrame owner;
    final PopupFrame popup;

    InventoryCleanerPopupOutsideClickFilter(InventoryCleanerPopupFrame inventoryCleanerPopupFrame, PopupFrame popupFrame) {
        this.owner = inventoryCleanerPopupFrame;
        this.popup = popupFrame;
    }

    @Override
    public boolean Q(Point point) {
        if (this.owner.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa().getBounds().R(point) && !this.popup.getBounds().R(point)) {
            return true;
        }
        return GuiMouseListener.super.Q(point);
    }

}

