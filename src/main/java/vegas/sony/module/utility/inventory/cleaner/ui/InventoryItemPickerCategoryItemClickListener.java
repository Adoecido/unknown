package vegas.sony.module.utility.inventory.cleaner.ui;

import vegas.sony.mapping.ItemMappingEntry;
import vegas.sony.module.utility.inventory.cleaner.ui.InventoryItemPickerPanel;
import vegas.sony.module.utility.inventory.cleaner.ui.ItemPickerSelection;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

public class InventoryItemPickerCategoryItemClickListener
implements GuiMouseListener {
    final ItemMappingEntry mappingEntry;
    final InventoryItemPickerPanel panel;

    public InventoryItemPickerCategoryItemClickListener(InventoryItemPickerPanel inventoryItemPickerPanel, ItemMappingEntry itemMappingEntry) {
        this.panel = inventoryItemPickerPanel;
        this.mappingEntry = itemMappingEntry;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        InventoryItemPickerPanel.select(this.panel, ItemPickerSelection.ofRight(this.mappingEntry));
    }
}
