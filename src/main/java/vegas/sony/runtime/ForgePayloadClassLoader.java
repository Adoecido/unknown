package vegas.sony.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads payload classes without delegating them back to Forge's routing loader.
 * Owns every class contained in its JAR, so it never needs package prefixes.
 */
public final class ForgePayloadClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public ForgePayloadClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> loaded = this.findLoadedClass(name);
            if (loaded == null) {
                try {
                    // Own every class inside this JAR. Forge routes payload
                    // packages here; delegating a miss to the parent would
                    // immediately route it back and recurse indefinitely.
                    loaded = this.findClass(name);
                }
                catch (ClassNotFoundException notFound) {
                    loaded = super.loadClass(name, false);
                }
            }
            if (resolve) {
                this.resolveClass(loaded);
            }
            return loaded;
        }
    }

    /**
     * Builds the exact package routes used by SecureModuleClassLoader. The
     * completed copy can be installed atomically without replacing Forge's
     * global fallback or exposing a HashMap resize to concurrent readers.
     */
    public Map<String, ClassLoader> buildPackageRoutingMap(
            Map<String, ClassLoader> currentRoutes) throws IOException {
        Map<String, ClassLoader> routes = new HashMap<String, ClassLoader>();
        if (currentRoutes != null) {
            routes.putAll(currentRoutes);
        }

        Set<String> packages = new HashSet<String>();
        for (URL url : this.getURLs()) {
            collectPackages(url, packages);
        }
        if (packages.isEmpty()) {
            throw new IOException("payload JAR contains no classes ");
        }
        for (String packageName : packages) {
            routes.put(packageName, this);
        }
        return routes;
    }

    private static void collectPackages(URL url, Set<String> packages)
            throws IOException {
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("unsupported payload URL: " + url);
        }
        File payload;
        try {
            URI uri = url.toURI();
            payload = new File(uri);
        }
        catch (URISyntaxException exception) {
            throw new IOException("invalid payload URL: " + url, exception);
        }
        if (!payload.isFile()) {
            throw new IOException("payload URL is not a JAR file: " + payload);
        }

        try (JarFile jar = new JarFile(payload)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (!entryName.endsWith(".class")) {
                    continue;
                }
                int separator = entryName.lastIndexOf('/');
                if (separator > 0) {
                    packages.add(entryName.substring(0, separator).replace('/', '.'));
                }
            }
        }
    }
}
