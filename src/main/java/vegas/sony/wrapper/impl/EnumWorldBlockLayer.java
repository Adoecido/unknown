package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEnumWorldBlockLayer;
import vegas.sony.wrapper.Wrapper;

public class EnumWorldBlockLayer
extends Wrapper {
    private static EnumWorldBlockLayer translucent;
    private static EnumWorldBlockLayer solid;

    public static EnumWorldBlockLayer translucent() {
        if (translucent == null) {
            translucent = new EnumWorldBlockLayer(MEnumWorldBlockLayer.getTranslucent(EnumWorldBlockLayer.sonyVegasInstance.getMappingsMapperCompat().worldBlockLayer));
        }
        return translucent;
    }


    public EnumWorldBlockLayer(Object handle) {
        super(handle);
    }

    public static EnumWorldBlockLayer solid() {
        if (solid == null) {
            solid = new EnumWorldBlockLayer(MEnumWorldBlockLayer.getSolid(EnumWorldBlockLayer.sonyVegasInstance.getMappingsMapperCompat().worldBlockLayer));
        }
        return solid;
    }
}

