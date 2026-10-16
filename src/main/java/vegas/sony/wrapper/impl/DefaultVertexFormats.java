package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MDefaultVertexFormats;
import vegas.sony.wrapper.Wrapper;

public class DefaultVertexFormats
extends Wrapper {
    public static DefaultVertexFormatBridge p() {
        return new DefaultVertexFormatBridge(MDefaultVertexFormats.getPositionColor(DefaultVertexFormats.sonyVegasInstance.getMappingsMapperCompat().DC));
    }

    public DefaultVertexFormats(Object object) {
        super(object);
    }
}
