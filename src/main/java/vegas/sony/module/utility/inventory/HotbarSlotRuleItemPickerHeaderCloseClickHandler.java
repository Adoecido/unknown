package vegas.sony.module.utility.inventory;

import vegas.sony.module.utility.inventory.HotbarSlotRuleItemPickerFrame;
import vegas.sony.ui.click.component.GuiClickListener;

class HotbarSlotRuleItemPickerHeaderCloseClickHandler
implements GuiClickListener {
    final HotbarSlotRuleItemPickerFrame pickerFrame;

    HotbarSlotRuleItemPickerHeaderCloseClickHandler(HotbarSlotRuleItemPickerFrame hotbarSlotRuleItemPickerFrame) {
        this.pickerFrame = hotbarSlotRuleItemPickerFrame;
    }

    @Override
    public void onPrimaryClick() {
        this.pickerFrame.commitSelection();
        this.pickerFrame.closePicker();
    }
}
