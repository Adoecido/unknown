package vegas.sony.module.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import vegas.sony.mapping.CapeTextureOverrideHook;

/** Tests resource decoding/host registration without Minecraft or an OpenGL context. */
public final class BundledCapeTextureProbe {
    public static final class Location {
        final String name;
        Location(String name) { this.name = name; }
    }

    public interface Texture { }

    public static final class Dynamic implements Texture {
        final BufferedImage image;
        public Dynamic(BufferedImage image) { this.image = image; }
    }

    public static class ObfuscatedManager {
        int uploads;
        final Map<Location, Texture> textures = new HashMap<Location, Texture>();
        public Location IlIl(String key, Dynamic texture) {
            Location location = new Location("dynamic/" + key + "_" + (++this.uploads));
            this.textures.put(location, texture);
            return location;
        }
        public Texture lIlI(Location location) { return this.textures.get(location); }
        public void IIll(Location location) { }
    }

    public static final class AmbiguousManager extends ObfuscatedManager {
        public Location other(String key, Dynamic texture) { throw new AssertionError("Must reject ambiguous API"); }
    }

    private enum Parts { CAPE, HAT }

    public static void main(String[] args) throws Exception {
        final int[] filters = {0};
        BundledCapeTexture cache = new BundledCapeTexture((texture, width, height) -> {
            require(width == height * 2 && (height == 512 || height == 1024),
                    "HD cape was downscaled");
            ++filters[0];
        });
        ObfuscatedManager manager = new ObfuscatedManager();
        String[] names = {"122", "123", "718", "436", "953"};
        Object first = null;
        for (String name : names) {
            Object location = cache.getOrLoad(manager, Location.class, name);
            require(location == cache.getOrLoad(manager, Location.class, name), "Texture was re-uploaded each frame");
            Dynamic texture = (Dynamic) manager.textures.get(location);
            require(texture != null && texture.image.getWidth() == texture.image.getHeight() * 2, "Invalid cape atlas");
            int expectedWidth = "436".equals(name) || "953".equals(name) ? 2048 : 1024;
            require(texture.image.getWidth() == expectedWidth, "Cape resolution changed: " + name);
            int front = texture.image.getRGB(expectedWidth * 6 / 64, expectedWidth * 9 / 64);
            int back = texture.image.getRGB(expectedWidth * 17 / 64, expectedWidth * 9 / 64);
            require((front >>> 24) != 0 && (back >>> 24) != 0,
                    "Cape front or back is empty: " + name);
            if (first == null) first = location;
        }
        require(manager.uploads == names.length, "Expected one upload per style");
        require(filters[0] == names.length, "Filtering must run once per uploaded texture");
        require(first == cache.getOrLoad(manager, Location.class, "122"), "Style switch lost cached texture");
        manager.textures.clear();
        require(first != cache.getOrLoad(manager, Location.class, "122"), "Deleted host texture was not re-registered");
        ObfuscatedManager replacement = new ObfuscatedManager();
        cache.getOrLoad(replacement, Location.class, "122");
        require(replacement.uploads == 1, "New texture manager reused another manager's handle");
        require(filters[0] == names.length + 2, "Reload and manager replacement must reapply filtering");
        BundledCapeTexture filterFailure = new BundledCapeTexture((texture, width, height) -> {
            throw new IllegalStateException("Simulated incompatible OpenGL texture");
        });
        ObfuscatedManager filterFailureManager = new ObfuscatedManager();
        Object unfilteredLocation = filterFailure.getOrLoad(filterFailureManager, Location.class,
                "122");
        require(filterFailureManager.textures.get(unfilteredLocation) != null,
                "A filter failure discarded a registered cape");
        require(unfilteredLocation == filterFailure.getOrLoad(filterFailureManager, Location.class,
                "122"), "A filter failure caused repeated uploads");
        try {
            cache.getOrLoad(replacement, Location.class, "missing_cape");
            throw new AssertionError("Missing PNG accepted");
        } catch (IOException expected) { }
        try {
            cache.getOrLoad(new AmbiguousManager(), Location.class, "122");
            throw new AssertionError("Ambiguous API accepted");
        } catch (NoSuchMethodException expected) { }
        Object original = new Object();
        require(CapeTextureOverrideHook.override(null, original) == original, "Hook did not preserve original before initialization");
        require(!CapeTextureOverrideHook.forceCapeVisible(null, Parts.CAPE, false), "Hook forced cape without a local player");
        require(CapeTextureOverrideHook.forceCapeVisible(null, Parts.HAT, true), "Hook altered another model part");
        require(CapeManager.shouldRenderOwnCape(0) && !CapeManager.shouldPublishOwnCape(0),
                "Only Me must render locally without publishing");
        require(!CapeManager.shouldRenderOwnCape(1) && CapeManager.shouldPublishOwnCape(1),
                "Others Only must publish without rendering locally");
        require(CapeManager.shouldRenderOwnCape(2) && CapeManager.shouldPublishOwnCape(2),
                "Me and Others must render and publish");
        for (String removed : new String[]{"classic", "aurora", "forest", "rose", "lava", "blue"}) {
            require(BundledCapeTextureProbe.class.getResource("/assets/sonyvegas/capes/" + removed + ".png") == null,
                    "Removed cape is still bundled: " + removed);
        }
        System.out.println("PASS: 5 HD capes with front/back, optional filtering, reload and fail-safe hooks");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
