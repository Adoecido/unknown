package vegas.sony.module.utility.inventory;

import vegas.sony.module.utility.inventory.HotbarSlotRuleItemPickerFrame;
import vegas.sony.module.utility.inventory.HotbarSlotRuleItemSearchComponent;
import vegas.sony.ui.click.component.GuiKeyTypedListener;

public class HotbarSlotRuleSearchInputKeyTypedListener
implements GuiKeyTypedListener {
    final HotbarSlotRuleItemPickerFrame pickerFrame;
    final HotbarSlotRuleItemSearchComponent searchComponent;

    public HotbarSlotRuleSearchInputKeyTypedListener(HotbarSlotRuleItemSearchComponent hotbarSlotRuleItemSearchComponent, HotbarSlotRuleItemPickerFrame hotbarSlotRuleItemPickerFrame) {
        this.searchComponent = hotbarSlotRuleItemSearchComponent;
        this.pickerFrame = hotbarSlotRuleItemPickerFrame;
    }

    @Override
    public void onKeyTyped(char c, int n) {
        this.pickerFrame.s(HotbarSlotRuleItemSearchComponent.g(this.searchComponent).isShowEditButton());
    }
}
