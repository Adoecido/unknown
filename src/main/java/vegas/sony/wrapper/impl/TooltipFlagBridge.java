package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.wrapper.Wrapper;

public class TooltipFlagBridge
extends Wrapper {

    public TooltipFlagBridge(Object handle) {
        super(handle);
    }

    public static TooltipFlagBridge searchTab() {
        if (ForgeVersion.MC_1_20_6.d()) {
            SonyVegas.notifyNativeStackTrace();
        }
        return new TooltipFlagBridge(TooltipFlagBridge.sonyVegasInstance.getMappings().creativeTabsSearch.getSearchTab());
    }
}

