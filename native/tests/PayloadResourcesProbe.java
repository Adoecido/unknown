import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import vegas.sony.runtime.PayloadResources;

public final class PayloadResourcesProbe {
    private static final String[] FONT_RESOURCES = {
        "proxima.ttf",
        "proximabd.ttf",
        "noto.ttf",
        "poppins_rg.ttf",
        "inter_medium.ttf",
        "inter_semibold.ttf",
        "poppins_medium.ttf",
        "poppins_semibold.ttf",
        "roboto_medium.ttf",
        "roboto_semibold.ttf",
        "jetbrains_medium.ttf",
        "jetbrains_semibold.ttf",
        "rajdhani_medium.ttf",
        "rajdhani_semibold.ttf",
        "ubuntu_medium.ttf",
        "ubuntu_semibold.ttf",
        "play_medium.ttf",
        "play_semibold.ttf",
        "raleway_medium.ttf",
        "raleway_semibold.ttf"
    };

    private PayloadResourcesProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected injection JAR path");
        }
        byte[] jar = Files.readAllBytes(Paths.get(arguments[0]));
        MemoryJarLoader memoryLoader = new MemoryJarLoader(
                jar, ClassLoader.getSystemClassLoader());
        Method install = PayloadResources.class.getDeclaredMethod(
                "installResourceLoader", ClassLoader.class);
        install.setAccessible(true);
        install.invoke(null, memoryLoader);

        ClassLoader transientLoader = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return "probe/context-only.txt".equals(name)
                        ? new ByteArrayInputStream(new byte[] { 1 }) : null;
            }
        };
        Thread.currentThread().setContextClassLoader(transientLoader);
        InputStream transientStream = PayloadResources.open(
                PayloadResourcesProbe.class, "/probe/context-only.txt");
        if (transientStream == null) {
            throw new AssertionError("context-loader fallback is unavailable");
        }
        transientStream.close();

        Thread.currentThread().setContextClassLoader(
                ClassLoader.getSystemClassLoader());
        for (String resource : FONT_RESOURCES) {
            assertAvailable(resource);
        }

        // Native bootstrap exits after start(), and unrelated game-thread
        // fallbacks must not replace the authoritative in-memory loader.
        for (String resource : FONT_RESOURCES) {
            assertAvailable(resource);
        }
        System.out.println("PayloadResources probe passed: fonts="
                + FONT_RESOURCES.length + ", cachedAfterContextChange=true");
    }

    private static void assertAvailable(String resource) {
        byte[] bytes = PayloadResources.readBytes(
                PayloadResourcesProbe.class, "/resources/" + resource);
        if (bytes == null || bytes.length == 0) {
            throw new AssertionError("missing payload resource: " + resource);
        }
    }
}
