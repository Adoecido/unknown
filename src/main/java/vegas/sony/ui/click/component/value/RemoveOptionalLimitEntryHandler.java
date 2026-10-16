package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.value.ListValueOptionsPanel;
import vegas.sony.value.OptionalLimitEntry;
import vegas.sony.value.OptionalLimitValue;

class RemoveOptionalLimitEntryHandler
implements GuiClickListener {
    final ListValueOptionsPanel optionsPanel;
    final OptionalLimitEntry entry;

    @Override
    public void onPrimaryClick() {
        ((OptionalLimitValue)ListValueOptionsPanel.getListValueCompat(this.optionsPanel)).removeEntry(this.entry);
        ListValueOptionsPanel.getListValueCompat(this.optionsPanel).notifyChanged();
        this.optionsPanel.refreshEntries();
    }

    RemoveOptionalLimitEntryHandler(ListValueOptionsPanel listValueOptionsPanel, OptionalLimitEntry optionalLimitEntry) {
        this.optionsPanel = listValueOptionsPanel;
        this.entry = optionalLimitEntry;
    }
}
