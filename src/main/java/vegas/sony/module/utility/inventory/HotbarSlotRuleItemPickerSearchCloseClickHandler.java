package vegas.sony.module.utility.inventory;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.utility.inventory.HotbarSlotRuleItemPickerFrame;
import vegas.sony.module.utility.inventory.HotbarSlotRuleItemSearchComponent;
import vegas.sony.ui.click.component.GuiClickListener;

public class HotbarSlotRuleItemPickerSearchCloseClickHandler
implements GuiClickListener {
    final HotbarSlotRuleItemSearchComponent searchComponent;

    @Override
    public void onPrimaryClick() {
        ClientSettings.getFrame(HotbarSlotRuleItemPickerFrame.class).commitSelection();
        ClientSettings.getFrame(HotbarSlotRuleItemPickerFrame.class).closePicker();
    }

    public HotbarSlotRuleItemPickerSearchCloseClickHandler(HotbarSlotRuleItemSearchComponent hotbarSlotRuleItemSearchComponent) {
        this.searchComponent = hotbarSlotRuleItemSearchComponent;
    }
}
