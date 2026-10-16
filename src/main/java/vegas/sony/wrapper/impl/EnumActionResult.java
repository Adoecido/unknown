package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class EnumActionResult
extends Wrapper {
    public EnumActionResult(Object actionResultHandle) {
        super(actionResultHandle);
    }

    public static EnumActionResult pass() {
        return new EnumActionResult(EnumActionResult.sonyVegasInstance.getMappingsMapperCompat().enumActionResult.getPass());
    }

    public static EnumActionResult fail() {
        return new EnumActionResult(EnumActionResult.sonyVegasInstance.getMappingsMapperCompat().enumActionResult.getFail());
    }

    public static EnumActionResult success() {
        return new EnumActionResult(EnumActionResult.sonyVegasInstance.getMappingsMapperCompat().enumActionResult.getSuccess());
    }
}
