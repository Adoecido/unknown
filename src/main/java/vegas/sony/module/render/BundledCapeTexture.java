package vegas.sony.module.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import vegas.sony.runtime.PayloadResources;

/** Registers payload PNGs with the host, whose resource packs cannot see our classloader. */
final class BundledCapeTexture {
    interface Filter {
        void apply(Object texture, int width, int height) throws Exception;
    }

    private final Filter filter;
    private final Map<String, Entry> textures = new HashMap<String, Entry>();
    private Object manager;
    private Method registerTexture;
    private Method getTexture;
    private Constructor<?> imageConstructor;

    BundledCapeTexture() {
        this(CapeTextureFilter::apply);
    }

    BundledCapeTexture(Filter filter) {
        this.filter = filter;
    }

    /** Must be called on the render thread (the getLocationCape render hook). */
    synchronized Object getOrLoad(Object textureManager, Class<?> locationType, String name) throws Exception {
        if (textureManager == null || locationType == null || name == null || !name.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Missing texture manager/type or invalid cape name");
        }
        if (this.manager != textureManager) {
            this.textures.clear();
            this.manager = null;
            this.resolveApi(textureManager.getClass(), locationType);
            this.manager = textureManager;
        }
        Entry cached = this.textures.get(name);
        if (cached != null && this.getTexture.invoke(textureManager, cached.location) == cached.texture) {
            return cached.location;
        }
        String resource = "/assets/lIlIIlIl/capes/" + name + ".png";
        BufferedImage image;
        try (InputStream input = PayloadResources.open(BundledCapeTexture.class, resource)) {
            if (input == null) throw new IOException("Bundled cape is missing: " + resource);
            image = ImageIO.read(input);
        }
        if (image == null) throw new IOException("Invalid cape PNG: " + resource);
        // Preserve the vanilla cape atlas layout at its native resolution:
        // the front and back panels begin at scaled UVs (1,1) and (12,1).
        // Moving either panel to the image origin crops the opposite face.
        // DynamicTexture(BufferedImage) uploads the pixels; the manager registers and owns it.
        Object texture = this.imageConstructor.newInstance(image);
        Object location = this.registerTexture.invoke(textureManager, "144_" + name, texture);
        if (location == null || this.getTexture.invoke(textureManager, location) != texture) {
            throw new IOException("Host did not register the dynamic cape texture");
        }
        // Filtering is only a visual improvement. Some client updates wrap or
        // replace the texture object, which can make its OpenGL id unavailable
        // even though TextureManager registered the cape successfully.
        try {
            this.filter.apply(texture, image.getWidth(), image.getHeight());
        } catch (Throwable ignored) {
        }
        this.textures.put(name, new Entry(location, texture));
        return location;
    }

    synchronized boolean ownsLocation(Object location) {
        if (location == null) return false;
        for (Entry entry : this.textures.values()) {
            if (entry.location == location || location.equals(entry.location)) return true;
        }
        return false;
    }

    private void resolveApi(Class<?> managerType, Class<?> locationType) throws ReflectiveOperationException {
        Method registration = null;
        Constructor<?> constructor = null;
        // 1.8.9: ResourceLocation getDynamicTextureLocation(String, DynamicTexture).
        // Resolve by the complete API shape, not an Allatori name or declaration order.
        for (Method method : managerType.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != locationType
                    || parameters.length != 2 || parameters[0] != String.class) continue;
            Constructor<?> candidate;
            try {
                candidate = parameters[1].getConstructor(BufferedImage.class);
            } catch (NoSuchMethodException notDynamicTexture) {
                continue;
            }
            if (registration != null) throw new NoSuchMethodException("Ambiguous dynamic texture registration");
            registration = method;
            constructor = candidate;
        }
        if (registration == null) throw new NoSuchMethodException("DynamicTexture(BufferedImage) API is unavailable");
        Method lookup = null;
        for (Method method : managerType.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) || parameters.length != 1 || parameters[0] != locationType
                    || method.getReturnType() == Object.class
                    || !method.getReturnType().isAssignableFrom(constructor.getDeclaringClass())) continue;
            if (lookup != null) throw new NoSuchMethodException("Ambiguous texture lookup");
            lookup = method;
        }
        if (lookup == null) throw new NoSuchMethodException("Texture lookup API is unavailable");
        registration.setAccessible(true);
        lookup.setAccessible(true);
        constructor.setAccessible(true);
        this.registerTexture = registration;
        this.getTexture = lookup;
        this.imageConstructor = constructor;
    }

    private static final class Entry {
        final Object location;
        final Object texture;

        Entry(Object location, Object texture) {
            this.location = location;
            this.texture = texture;
        }
    }
}
