package vegas.sony.utils.render;

import org.lwjgl.opengl.GL11;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.AbstractClientPlayer;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.GlStateManager;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.ResourceLocation;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

/**
 * Draws the 8x8 face (plus the hat overlay) out of a player skin texture.
 *
 * Skins reach the client as 64x64 since 1.8 - legacy 64x32 sheets are converted on download - so the
 * face and hat always sit at the same normalized coordinates.
 */
public final class PlayerHeadRenderer {
    private static final float SKIN_SIZE = 64.0f;
    private static final float FACE_MIN_U = 8.0f / SKIN_SIZE;
    private static final float FACE_MIN_V = 8.0f / SKIN_SIZE;
    private static final float FACE_MAX_U = 16.0f / SKIN_SIZE;
    private static final float FACE_MAX_V = 16.0f / SKIN_SIZE;
    private static final float HAT_MIN_U = 40.0f / SKIN_SIZE;
    private static final float HAT_MIN_V = 8.0f / SKIN_SIZE;
    private static final float HAT_MAX_U = 48.0f / SKIN_SIZE;
    private static final float HAT_MAX_V = 16.0f / SKIN_SIZE;

    /** GUI rows redraw every frame, so the world scan behind a lookup is cached briefly. */
    private static final long SKIN_CACHE_TTL_MS = 500L;
    private static final java.util.Map<String, CachedSkin> SKIN_CACHE =
            new java.util.concurrent.ConcurrentHashMap<String, CachedSkin>();

    private PlayerHeadRenderer() {
    }

    /**
     * Resolves the skin of the loaded player carrying {@code playerName}, falling back to the default
     * skin when that player is not in render distance.
     */
    public static ResourceLocation resolveSkin(String playerName) {
        String key = normalize(playerName);
        if (key.isEmpty()) return null;
        long now = System.currentTimeMillis();
        CachedSkin cached = SKIN_CACHE.get(key);
        if (cached != null && now - cached.resolvedAt < SKIN_CACHE_TTL_MS) {
            return cached.skin;
        }
        ResourceLocation resolved = resolveSkinUncached(playerName);
        SKIN_CACHE.put(key, new CachedSkin(resolved, now));
        return resolved;
    }

    public static void invalidateCache() {
        SKIN_CACHE.clear();
    }

    private static ResourceLocation resolveSkinUncached(String playerName) {
        AbstractClientPlayer player = findPlayer(playerName);
        if (player != null) {
            try {
                ResourceLocation skin = player.O();
                if (skin != null && skin.isNotNull()) {
                    return skin;
                }
            } catch (Throwable ignored) {}
        }
        try {
            return EntityModelRenderCache.getDefaultSkinTexture();
        } catch (Throwable t) {
            return null;
        }
    }

    private static AbstractClientPlayer findPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;
        String wanted = normalize(playerName);
        if (wanted.isEmpty()) return null;
        try {
            if (Minecraft.theWorld().isNull()) return null;
            List<?> worldEntities = Minecraft.theWorld().z();
            if (worldEntities == null) return null;
            for (Object handle : worldEntities) {
                try {
                    Entity entity = new Entity(handle);
                    if (!entity.isInstance(MappedClasses.Yl)) continue;
                    EntityLivingBase living = new EntityLivingBase(handle);
                    if (living.isNull()) continue;
                    if (!wanted.equals(normalize(living.getName()))) continue;
                    return new AbstractClientPlayer(handle);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String normalize(String name) {
        if (name == null) return "";
        return name.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
    }

    private static final class CachedSkin {
        final ResourceLocation skin;
        final long resolvedAt;

        CachedSkin(ResourceLocation skin, long resolvedAt) {
            this.skin = skin;
            this.resolvedAt = resolvedAt;
        }
    }

    public static void drawHead(String playerName, float x, float y, float size, Color tint) {
        drawHead(resolveSkin(playerName), x, y, size, tint);
    }

    public static void drawHead(ResourceLocation skin, float x, float y, float size, Color tint) {
        if (skin == null || skin.isNull() || size <= 0.0f) {
            return;
        }
        try {
            if (GuiRenderPrimitives.d()) {
                drawBuffered(skin, x, y, size, tint);
            } else {
                drawLegacy(skin, x, y, size, tint);
            }
        } catch (Throwable ignored) {}
    }

    private static void drawBuffered(ResourceLocation skin, float x, float y, float size, Color tint) {
        vegas.sony.wrapper.impl.TextureObject textureObject = Minecraft.getTextureManager().getTexture(skin);
        if (textureObject == null || textureObject.isNull()) {
            return;
        }
        int textureId = textureObject.getId();
        if (textureId <= 0) {
            return;
        }
        queueQuad(textureId, x, y, size, tint, FACE_MIN_U, FACE_MIN_V, FACE_MAX_U, FACE_MAX_V);
        queueQuad(textureId, x, y, size, tint, HAT_MIN_U, HAT_MIN_V, HAT_MAX_U, HAT_MAX_V);
    }

    private static void queueQuad(int textureId, float x, float y, float size, Color tint,
                                  float minU, float minV, float maxU, float maxV) {
        GlImageTexture texture = new GlImageTexture(textureId, (int) SKIN_SIZE, (int) SKIN_SIZE, minU, minV, maxU, maxV);
        // textureWidth == textureHeight keeps addTexturedRect's square-aspect correction a no-op.
        RenderBatchManager.getInstance().queueGuiBatch(new RenderBatchBuilder()
                .setTexture(texture)
                .addTexturedRect(x, y, size, size, SKIN_SIZE, SKIN_SIZE, minU, minV, maxU, maxV, tint));
    }

    private static void drawLegacy(ResourceLocation skin, float x, float y, float size, Color tint) {
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean textureWasEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GL11.glPushMatrix();
        if (!blendWasEnabled) {
            GlStateManager.enableBlend();
        }
        if (!textureWasEnabled) {
            GlStateManager.enableTexture2D();
        }
        try {
            Minecraft.getTextureManager().bindTexture(skin);
            GlStateManager.color(tint.getRed() / 255.0f, tint.getGreen() / 255.0f,
                    tint.getBlue() / 255.0f, tint.getAlpha() / 255.0f);
            drawQuad(x, y, size, FACE_MIN_U, FACE_MIN_V, FACE_MAX_U, FACE_MAX_V);
            drawQuad(x, y, size, HAT_MIN_U, HAT_MIN_V, HAT_MAX_U, HAT_MAX_V);
        } finally {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            if (!blendWasEnabled) {
                GlStateManager.disableBlend();
            }
            GL11.glPopMatrix();
        }
    }

    private static void drawQuad(float x, float y, float size, float minU, float minV, float maxU, float maxV) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(minU, minV);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(minU, maxV);
        GL11.glVertex2f(x, y + size);
        GL11.glTexCoord2f(maxU, maxV);
        GL11.glVertex2f(x + size, y + size);
        GL11.glTexCoord2f(maxU, minV);
        GL11.glVertex2f(x + size, y);
        GL11.glEnd();
    }
}
