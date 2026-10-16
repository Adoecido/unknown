package vegas.sony.wrapper.impl;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.Wrapper;

public class FontGlyph
extends Wrapper {
    public boolean isGlyphInfo() {
        return MappedClasses.v != null && MappedClasses.v.isInstance(this.I);
    }

    public GlyphInfo asGlyphInfo() {
        if (this.isGlyphInfo()) {
            return new GlyphInfo(this.I);
        }
        return null;
    }

    public FontGlyphInfo getInfo() {
        Object info = FontGlyph.sonyVegasInstance.getMappingsMapperCompat().De.getInfo(this.I);
        return info != null ? new FontGlyphInfo(info) : null;
    }


    public FontGlyph(Object wrappedObject) {
        super(wrappedObject);
    }
}

