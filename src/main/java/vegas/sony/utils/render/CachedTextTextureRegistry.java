package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import vegas.sony.utils.render.CachedTextTexture;
import vegas.sony.utils.render.CachedTextTextureKey;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.HashMap;

public class CachedTextTextureRegistry {
    private static String legacyMarker;
    static HashMap<CachedTextTextureKey, CachedTextTexture> cache;

    public static String getLegacyMarker() {
        return legacyMarker;
    }

    private static void createTexture(CachedTextTextureKey cacheKey) {
        CachedTextTexture texture = new CachedTextTexture();
        texture.build(cacheKey.getText(), cacheKey.getColor());
        cache.put(cacheKey, texture);
    }


    public static void setLegacyMarker(String legacyMarker) {
        CachedTextTextureRegistry.legacyMarker = legacyMarker;
    }

    public static void renderText(String text, float x, float y, int color) {
        try {
            CachedTextTextureKey cacheKey = new CachedTextTextureKey(text, color);
            CachedTextTextureRegistry.ensureCached(text, color);
            int textWidth = Minecraft.getFontRenderer().getStringWidth(text);
            int textHeight = Minecraft.getFontRenderer().FONT_HEIGHT(text);
            cache.get(cacheKey).render(x, y, textWidth, textHeight);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    public static void ensureCached(String text, int color) {
        CachedTextTextureKey cacheKey = new CachedTextTextureKey(text, color);
        if (!cache.containsKey(cacheKey)) {
            CachedTextTextureRegistry.createTexture(cacheKey);
        }
    }

    static {
        cache = new HashMap();
        CachedTextTextureRegistry.setLegacyMarker(null);
    }
}

