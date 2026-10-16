package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.value.ListValueOptionsPanel;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.value.LimitValue;

public class RemoveLimitValueEntryHandler
implements GuiClickListener {
    final ItemLimitData entry;
    final ListValueOptionsPanel optionsPanel;

    public RemoveLimitValueEntryHandler(ListValueOptionsPanel listValueOptionsPanel, ItemLimitData itemLimitData) {
        this.optionsPanel = listValueOptionsPanel;
        this.entry = itemLimitData;
    }

    @Override
    public void onPrimaryClick() {
        ((LimitValue)ListValueOptionsPanel.getListValueCompat(this.optionsPanel)).removeEntry(this.entry);
        ListValueOptionsPanel.getListValueCompat(this.optionsPanel).notifyChanged();
        this.optionsPanel.refreshEntries();
    }
}
