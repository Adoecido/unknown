package vegas.sony.ui.font;

import vegas.sony.ui.font.SmoothFontRenderer;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmoothFontRendererWidthCache
extends LinkedHashMap<String, Double> {
    final SmoothFontRenderer s;

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Double> entry) {
        return this.size() > 5120;
    }

    public SmoothFontRendererWidthCache(SmoothFontRenderer smoothFontRenderer, int n, float f, boolean bl) {
        super(n, f, bl);
        this.s = smoothFontRenderer;
    }

}
