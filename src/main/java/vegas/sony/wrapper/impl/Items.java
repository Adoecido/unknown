package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MItems;
import vegas.sony.wrapper.Wrapper;

public class Items
extends Wrapper {
    public static Items perspective() {
        return new Items(MItems.getPerspective(Items.sonyVegasInstance.getMappingsMapperCompat().projectionType));
    }

    public static Items orthographic() {
        return new Items(MItems.getOrthographic(Items.sonyVegasInstance.getMappingsMapperCompat().projectionType));
    }

    public Items(Object handle) {
        super(handle);
    }
}
