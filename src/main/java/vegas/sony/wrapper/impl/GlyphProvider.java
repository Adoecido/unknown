package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MGlyphProvider;
import vegas.sony.wrapper.Wrapper;

public class GlyphProvider
extends Wrapper {
    public GlyphProvider(Object handle) {
        super(handle);
    }

    public FontGlyph getGlyph(int codePoint) {
        return new FontGlyph(MGlyphProvider.getGlyph(GlyphProvider.sonyVegasInstance.getMappingsMapperCompat().glyphSource, this.I, codePoint));
    }
}
