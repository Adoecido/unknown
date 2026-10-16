package vegas.sony.mapping;

import javassist.ClassPath;
import javassist.LoaderClassPath;
import java.io.InputStream;
import java.net.URL;

/**
 * Lunar-only Javassist class path.
 *
 * <p>Lunar's Ichor/Genesis loaders serve the ORIGINAL notch bytes when asked
 * for a remapped (MCP) resource path. A plain LoaderClassPath would hand
 * those mismatched bytes to Javassist, which then fails every stackmap
 * computation touching the entity hierarchy ("pk found in
 * net/minecraft/entity/Entity.class"). This filter hides the game namespaces
 * from the game loader so those types resolve through the live, consistent
 * loaders instead, while payload-owned and library types keep resolving
 * from the game loader exactly as before.
 *
 * <p>Used only when {@code NativeBridge.isLunar189Runtime()} is true; every
 * other runtime keeps the historical blanket LoaderClassPath.
 */
public final class LunarFilteredLoaderClassPath implements ClassPath {
    private final LoaderClassPath delegate;

    public LunarFilteredLoaderClassPath(ClassLoader loader) {
        this.delegate = new LoaderClassPath(loader);
    }

    private static boolean denied(String classname) {
        return classname != null
                && (classname.startsWith("net.minecraft.")
                        || classname.startsWith("net.minecraftforge."));
    }

    @Override
    public InputStream openClassfile(String classname) {
        if (denied(classname)) {
            return null;
        }
        try {
            return this.delegate.openClassfile(classname);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public URL find(String classname) {
        if (denied(classname)) {
            return null;
        }
        try {
            return this.delegate.find(classname);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
