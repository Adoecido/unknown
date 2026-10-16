package vegas.sony.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Resolves payload resources from both normal JAR and memory injection modes. */
public final class PayloadResources {
    private static volatile ClassLoader resourceLoader;

    private PayloadResources() {
    }

    static void installResourceLoader(ClassLoader loader) {
        if (loader != null) {
            resourceLoader = loader;
        }
    }

    public static InputStream open(Class<?> anchor, String resourceName) {
        if (anchor == null || resourceName == null) {
            return null;
        }
        InputStream runtimeMapping = openRuntimeMapping(resourceName);
        if (runtimeMapping != null) {
            return runtimeMapping;
        }
        String classLoaderName = toClassLoaderName(anchor, resourceName);
        ClassLoader cachedLoader = resourceLoader;
        if (cachedLoader != null) {
            InputStream stream = cachedLoader.getResourceAsStream(classLoaderName);
            if (stream != null) {
                return stream;
            }
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null && contextLoader != cachedLoader) {
            InputStream stream = contextLoader.getResourceAsStream(classLoaderName);
            if (stream != null) {
                // An explicitly installed MemoryJarLoader is authoritative.
                // Replacing it with a transient game-thread context loader
                // makes later lookups fail after an initially successful one.
                if (cachedLoader == null) {
                    resourceLoader = contextLoader;
                }
                return stream;
            }
        }
        ClassLoader anchorLoader = anchor.getClassLoader();
        if (anchorLoader != null && anchorLoader != contextLoader
                && anchorLoader != cachedLoader) {
            InputStream stream = anchorLoader.getResourceAsStream(classLoaderName);
            if (stream != null) {
                return stream;
            }
        }
        InputStream anchorStream = anchor.getResourceAsStream(resourceName);
        if (anchorStream != null) {
            return anchorStream;
        }

        // During memory injection the payload classes live in the game's
        // ClassLoader, while non-class entries remain in the embedded JAR.
        // gfb is the registered bridge to that JAR and was the original
        // resource path used by SonyVegas before ClassLoader support was added.
        try {
            byte[] embedded = NativeBridge.gfb(classLoaderName);
            if (embedded != null) {
                return new ByteArrayInputStream(embedded);
            }
        }
        catch (Throwable ignored) {
            // Normal JAR/test execution has no registered native bridge.
        }
        return null;
    }

    private static InputStream openRuntimeMapping(String resourceName) {
        String prefix = "/mappings/cheatbreaker189/";
        if (!resourceName.startsWith(prefix)) {
            return null;
        }
        try {
            byte[] bytes = NativeBridge.gfb(resourceName);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    /** Reads a payload resource while preserving the old null-on-failure API. */
    public static byte[] readBytes(Class<?> anchor, String resourceName) {
        InputStream stream = open(anchor, resourceName);
        if (stream == null) {
            return null;
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
        catch (IOException ignored) {
            return null;
        }
        finally {
            try {
                stream.close();
            }
            catch (IOException ignored) {
                // Keep resource lookup compatible with the previous native API.
            }
        }
    }

    private static String toClassLoaderName(Class<?> anchor,
            String resourceName) {
        if (resourceName.startsWith("/")) {
            return resourceName.substring(1);
        }
        String className = anchor.getName();
        int packageSeparator = className.lastIndexOf('.');
        if (packageSeparator < 0) {
            return resourceName;
        }
        return className.substring(0, packageSeparator).replace('.', '/')
                + '/' + resourceName;
    }
}
