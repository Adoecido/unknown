package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class FontGlyphInfo
extends Wrapper {
    public float getShadowOffset() {
        return FontGlyphInfo.sonyVegasInstance.getMappingsMapperCompat().hZ.getShadowOffset(this.I);
    }

    public FontGlyphInfo(Object wrappedObject) {
        super(wrappedObject);
    }

    public float getBoldOffset() {
        return FontGlyphInfo.sonyVegasInstance.getMappingsMapperCompat().hZ.getBoldOffset(this.I);
    }

    public float getAdvance(boolean bold) {
        return FontGlyphInfo.sonyVegasInstance.getMappingsMapperCompat().hZ.getAdvance(this.I, bold);
    }

    public float getAdvance() {
        return FontGlyphInfo.sonyVegasInstance.getMappingsMapperCompat().hZ.getAdvance(this.I);
    }
}
