package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ITooltipFlag
extends Wrapper {
    public static ITooltipFlag water() {
        return new ITooltipFlag(ITooltipFlag.sonyVegasInstance.getMappingsMapperCompat().clipContextFluidMode.getWater());
    }

    public ITooltipFlag(Object handle) {
        super(handle);
    }
}
