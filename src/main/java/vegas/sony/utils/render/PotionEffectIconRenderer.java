package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.PotionEffectIcon;
import vegas.sony.utils.render.PotionEffectIconKey;
import vegas.sony.utils.render.PotionEffectIconRenderBackend;
import vegas.sony.utils.render.PotionEffectIconTexture;
import vegas.sony.wrapper.impl.PotionEffect;
import java.util.HashMap;

public class PotionEffectIconRenderer {
    private static String legacyMarker;
    static HashMap<PotionEffectIconKey, PotionEffectIconRenderBackend> cache;

    public static void render(PotionEffect effect, float x, float y, int width, int height, float opacity, boolean worldSpace) {
        try {
            PotionEffectIconKey cacheKey = new PotionEffectIconKey(effect.C());
            PotionEffectIconRenderer.ensureCached(effect.C());
            cache.get(cacheKey).renderQueued(x, y, width, height, opacity, worldSpace);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }


    public static void ensureCached(int effectId) {
        PotionEffectIconKey cacheKey = new PotionEffectIconKey(effectId);
        if (!cache.containsKey(cacheKey)) {
            PotionEffectIconRenderer.createRenderer(PotionEffect.o(effectId, 100, 0), cacheKey);
        }
    }

    public static void setLegacyMarker(String legacyMarker) {
        PotionEffectIconRenderer.legacyMarker = legacyMarker;
    }

    public static String getLegacyMarker() {
        return legacyMarker;
    }

    public static void clear() {
        for (PotionEffectIconRenderBackend renderer : cache.values()) {
            renderer.dispose();
        }
        cache.clear();
    }

    public static void render(PotionEffect effect, float x, float y, int width, int height, float opacity) {
        PotionEffectIconRenderer.render(effect, x, y, width, height, opacity, false);
    }

    private static void createRenderer(PotionEffect effect, PotionEffectIconKey cacheKey) {
        PotionEffectIconRenderBackend renderer = GuiRenderPrimitives.d() ? new PotionEffectIconTexture() : new PotionEffectIcon();
        renderer.capture(effect);
        cache.put(cacheKey, renderer);
    }

    static {
        cache = new HashMap();
        PotionEffectIconRenderer.setLegacyMarker(null);
    }
}

