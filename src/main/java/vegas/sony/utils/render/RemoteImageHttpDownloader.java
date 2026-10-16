package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class RemoteImageHttpDownloader {
    private static final Map<String, byte[]> imageCache;
    private static final String CACHE_DIRECTORY;
    private static boolean enabled;

    public static byte[] loadCachedImage(String imageName) {
        // No disk footprint: only pre-existing cache entries are read. The
        // directory itself is never created by the client.
        File cacheDirectory = new File(CACHE_DIRECTORY);
        if (!cacheDirectory.isDirectory()) {
            imageCache.put(imageName, null);
            return null;
        }
        if (imageCache.containsKey(imageName)) {
            return imageCache.get(imageName);
        }
        try {
            byte[] imageData = Files.readAllBytes(new File(CACHE_DIRECTORY + imageName + ".png").toPath());
            imageCache.put(imageName, imageData);
            return imageData;
        }
        catch (IOException exception) {
            SonyVegas.logThrowable(exception);
            imageCache.put(imageName, null);
            return null;
        }
    }

    public static void setEnabled(boolean enabled) {
        RemoteImageHttpDownloader.enabled = enabled;
    }

    private static Exception propagateException(Exception exception) {
        return exception;
    }

    static {
        RemoteImageHttpDownloader.setEnabled(true);
        CACHE_DIRECTORY = System.getProperty("user.home") + File.separator + "txcache" + File.separator;
        imageCache = new LinkedHashMap<String, byte[]>();
    }

    public static boolean legacyAlwaysFalseCheck() {
        boolean enabled = RemoteImageHttpDownloader.isEnabled();
        return false;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
