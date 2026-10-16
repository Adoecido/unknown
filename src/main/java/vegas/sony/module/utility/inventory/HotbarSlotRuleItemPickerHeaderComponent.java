package vegas.sony.module.utility.inventory;

import vegas.sony.module.utility.inventory.HotbarSlotRuleItemPickerFrame;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.impl.profile.PublicProfilesFrameHeaderActionComponent;

public class HotbarSlotRuleItemPickerHeaderComponent
extends PublicProfilesFrameHeaderActionComponent {
    final HotbarSlotRuleItemPickerFrame pickerFrame;

    @Override
    public double x() {
        return 330.0;
    }

    public HotbarSlotRuleItemPickerHeaderComponent(HotbarSlotRuleItemPickerFrame hotbarSlotRuleItemPickerFrame, Frame frame, String string, String string2) {
        super(frame, string, string2);
        this.pickerFrame = hotbarSlotRuleItemPickerFrame;
    }
}
