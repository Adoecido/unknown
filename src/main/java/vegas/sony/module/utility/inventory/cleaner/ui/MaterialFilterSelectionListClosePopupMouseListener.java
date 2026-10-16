package vegas.sony.module.utility.inventory.cleaner.ui;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.utility.inventory.cleaner.ui.MaterialFilterSelectionList;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.AnchoredPopupFrame;
import java.awt.Point;

class MaterialFilterSelectionListClosePopupMouseListener
implements GuiMouseListener {
    final AnchoredPopupFrame popup;

    MaterialFilterSelectionListClosePopupMouseListener(MaterialFilterSelectionList materialFilterSelectionList, AnchoredPopupFrame anchoredPopupFrame) {
        this.popup = anchoredPopupFrame;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (!this.popup.w$src$Z$e457mb() && !this.popup.D$src$Lvegas_sony_ui_click_component_GuiComponent_$srx612().w$src$Z$e457mb()) {
            ClientSettings.removePopup(this.popup);
        }
    }

}

