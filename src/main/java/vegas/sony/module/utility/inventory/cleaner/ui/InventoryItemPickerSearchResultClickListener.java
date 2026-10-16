package vegas.sony.module.utility.inventory.cleaner.ui;

import vegas.sony.mapping.ItemMappingEntry;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.utility.inventory.cleaner.ui.InventoryItemPickerPanel;
import vegas.sony.module.utility.inventory.cleaner.ui.ItemPickerSelection;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

public class InventoryItemPickerSearchResultClickListener
implements GuiMouseListener {
    final InventoryItemPickerPanel panel;
    final ItemMappingEntry mappingEntry;

    private void selectItem(ItemMappingEntry itemMappingEntry) {
        InventoryItemPickerPanel.select(this.panel, ItemPickerSelection.ofRight(itemMappingEntry));
        InventoryItemPickerPanel.getSelectedItemIds(this.panel).remove(itemMappingEntry.getResourceKey());
        this.panel.showSearchView();
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        ClientSettings.UI_EXECUTOR.execute(() -> this.selectItem(this.mappingEntry));
    }

    public InventoryItemPickerSearchResultClickListener(InventoryItemPickerPanel inventoryItemPickerPanel, ItemMappingEntry itemMappingEntry) {
        this.panel = inventoryItemPickerPanel;
        this.mappingEntry = itemMappingEntry;
    }
}

