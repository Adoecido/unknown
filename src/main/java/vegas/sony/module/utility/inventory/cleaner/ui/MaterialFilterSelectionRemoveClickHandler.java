package vegas.sony.module.utility.inventory.cleaner.ui;

import vegas.sony.module.utility.inventory.cleaner.ui.MaterialFilterSelectionList;
import vegas.sony.module.utility.inventory.cleaner.ui.MaterialFilterSelectionRow;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

class MaterialFilterSelectionRemoveClickHandler
implements GuiMouseListener {
    final MaterialFilterSelectionList list;
    final MaterialFilterSelectionRow row;

    MaterialFilterSelectionRemoveClickHandler(MaterialFilterSelectionList materialFilterSelectionList, MaterialFilterSelectionRow materialFilterSelectionRow) {
        this.list = materialFilterSelectionList;
        this.row = materialFilterSelectionRow;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        this.list.removeSelectionRow(this.row);
    }
}
