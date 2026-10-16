import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

public final class MemoryJarLoaderProbe {
    private MemoryJarLoaderProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected injection JAR path");
        }
        byte[] jar = Files.readAllBytes(Paths.get(arguments[0]));
        MemoryJarLoader loader = new MemoryJarLoader(
                jar, ClassLoader.getSystemClassLoader());
        String[] names = loader.getClassNames();
        if (names.length == 0) {
            throw new AssertionError("no classes exposed");
        }
        try {
            MemoryJarLoader.class.getDeclaredMethod("findClass", String.class);
            throw new AssertionError("resource loader still defines classes");
        } catch (NoSuchMethodException expected) {
            // Resource-only behavior is intentional.
        }
        InputStream manifest = loader.getResourceAsStream(
                "META-INF/MANIFEST.MF");
        if (manifest == null) {
            throw new AssertionError("manifest resource is unavailable");
        }
        manifest.close();
        URL manifestUrl = loader.getResource("META-INF/MANIFEST.MF");
        if (manifestUrl == null
                || !"memoryjar".equals(manifestUrl.getProtocol())) {
            throw new AssertionError("manifest URL is unavailable");
        }
        InputStream manifestUrlStream = manifestUrl.openStream();
        if (manifestUrlStream.read() < 0) {
            throw new AssertionError("manifest URL is empty");
        }
        manifestUrlStream.close();
        Enumeration<URL> manifests = loader.getResources(
                "META-INF/MANIFEST.MF");
        boolean foundMemoryResource = false;
        while (manifests.hasMoreElements()) {
            if ("memoryjar".equals(manifests.nextElement().getProtocol())) {
                foundMemoryResource = true;
            }
        }
        if (!foundMemoryResource) {
            throw new AssertionError("manifest enumeration omits memory JAR");
        }

        HashMap<String, Integer> positions = new HashMap<String, Integer>();
        for (int index = 0; index < names.length; ++index) {
            positions.put(names[index], Integer.valueOf(index));
            if (loader.getClassBytes(names[index]) == null) {
                throw new AssertionError("missing bytes for " + names[index]);
            }
        }
        Method dependencies = MemoryJarLoader.class.getDeclaredMethod(
                "directHierarchyDependencies", byte[].class);
        dependencies.setAccessible(true);
        HashSet<String> externalHierarchy = new HashSet<String>();
        for (int index = 0; index < names.length; ++index) {
            String[] direct = (String[])dependencies.invoke(null,
                    new Object[] { loader.getClassBytes(names[index]) });
            for (int dependencyIndex = 0;
                    dependencyIndex < direct.length; ++dependencyIndex) {
                Integer position = positions.get(direct[dependencyIndex]);
                if (position != null && position.intValue() >= index) {
                    throw new AssertionError("dependency order is invalid: "
                            + direct[dependencyIndex] + " before " + names[index]);
                }
                if (position == null && direct[dependencyIndex] != null
                        && !isBootstrapClass(direct[dependencyIndex])) {
                    externalHierarchy.add(direct[dependencyIndex]);
                }
            }
        }
        System.out.println("MemoryJarLoader probe passed: classes="
                + names.length + ", jarBytes=" + jar.length
                + ", externalHierarchy=" + externalHierarchy);
    }

    private static boolean isBootstrapClass(String name) {
        try {
            Class.forName(name, false, null);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
