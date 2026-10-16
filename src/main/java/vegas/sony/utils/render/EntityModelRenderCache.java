package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import vegas.sony.utils.render.EntityModelFramebufferRenderer;
import vegas.sony.utils.render.EntityModelRenderBackend;
import vegas.sony.utils.render.EntityModelRenderCacheKey;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.Post117EntityModelFramebufferRenderer;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.ResourceLocation;
import java.awt.Color;
import java.util.HashMap;

public class EntityModelRenderCache {
    private static boolean enabled;
    static HashMap<EntityModelRenderCacheKey, EntityModelRenderBackend> renderers;

    public static void setEnabled(boolean enabled) {
        EntityModelRenderCache.enabled = enabled;
    }

    public static void renderEntity(EntityLivingBase entity, float x, float y, int width, int height, Color color, float cornerRadius) {
        try {
            EntityModelRenderCacheKey cacheKey = new EntityModelRenderCacheKey(entity);
            EntityModelRenderCache.ensureEntityCached(entity);
            renderers.get(cacheKey).render(x, y, width, height, color, cornerRadius);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    public static void ensureTextureCached(ResourceLocation texture, String identifier) {
        EntityModelRenderCacheKey cacheKey = new EntityModelRenderCacheKey(identifier);
        if (!renderers.containsKey(cacheKey)) {
            EntityModelRenderCache.cacheTexture(texture, cacheKey);
        }
    }

    private static void cacheTexture(ResourceLocation texture, EntityModelRenderCacheKey cacheKey) {
        EntityModelRenderBackend renderer = createRenderer();
        renderer.captureTexture(texture);
        renderers.put(cacheKey, renderer);
    }

    public static void ensureEntityCached(EntityLivingBase entity) {
        EntityModelRenderCacheKey cacheKey = new EntityModelRenderCacheKey(entity);
        if (!renderers.containsKey(cacheKey)) {
            EntityModelRenderCache.cacheEntity(entity, cacheKey);
        }
    }

    public static void clear() {
        for (EntityModelRenderBackend renderer : renderers.values()) {
            renderer.dispose();
        }
        renderers.clear();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isSupported() {
        boolean cacheEnabled = EntityModelRenderCache.isEnabled();
        return true;
    }

    public static ResourceLocation getDefaultSkinTexture() {
        String texturePath = ForgeVersion.MC_1_21_4.d() ? "textures/entity/player/wide/steve.png" : "textures/entity/steve.png";
        return ResourceLocation.create(texturePath);
    }


    private static void cacheEntity(EntityLivingBase entity, EntityModelRenderCacheKey cacheKey) {
        EntityModelRenderBackend renderer = createRenderer();
        renderer.captureEntity(entity);
        renderers.put(cacheKey, renderer);
    }

    private static EntityModelRenderBackend createRenderer() {
        return GuiRenderPrimitives.d() ? new Post117EntityModelFramebufferRenderer() : new EntityModelFramebufferRenderer();
    }

    public static void renderTexture(ResourceLocation texture, String identifier, float x, float y, int width, int height, Color color, float cornerRadius) {
        try {
            EntityModelRenderCacheKey cacheKey = new EntityModelRenderCacheKey(identifier);
            EntityModelRenderCache.ensureTextureCached(texture, identifier);
            renderers.get(cacheKey).render(x, y, width, height, color, cornerRadius);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    static {
        EntityModelRenderCache.setEnabled(false);
        renderers = new HashMap();
    }
}

