package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import vegas.sony.unmap.ImageParser$Format;
import vegas.sony.utils.render.GlImageTexture;
import vegas.sony.utils.render.ImageRenderer;
import vegas.sony.utils.render.RemoteImageTextureLoader;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

class RemoteImageTextureCache {
    private final int imageSize;
    private ConcurrentLinkedQueue<String> pendingUsernames = new ConcurrentLinkedQueue();
    private ConcurrentHashMap<String, GlImageTexture> textures;
    private ConcurrentHashMap<String, byte[]> downloadedImages = new ConcurrentHashMap();

    void processPendingDownloads() {
        while (!this.pendingUsernames.isEmpty()) {
            this.download(this.pendingUsernames.poll());
        }
    }

    public RemoteImageTextureCache(int imageSize) {
        this.textures = new ConcurrentHashMap();
        this.imageSize = imageSize;
    }

    byte[] getDownloadedImage(String username) {
        if (this.downloadedImages.containsKey(username)) {
            return this.downloadedImages.get(username);
        }
        if (!this.pendingUsernames.contains(username)) {
            this.pendingUsernames.add(username);
        }
        return null;
    }

    private static Exception identityException(Exception exception) {
        return exception;
    }

    GlImageTexture getTexture(String username) {
        if (this.textures.containsKey(username)) {
            return this.textures.get(username);
        }
        byte[] imageData = this.getDownloadedImage(username);
        if (imageData == null) {
            return null;
        }
        GlImageTexture texture = null;
        try {
            texture = new GlImageTexture(new ByteArrayInputStream(imageData), 9729, ImageParser$Format.RGBA);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
            texture = ImageRenderer.loadResource("default_user", false, false);
        }
        this.textures.put(username, texture);
        return texture;
    }

    void download(String username) {
        byte[] imageData = RemoteImageTextureLoader.download("https://minotar.net/avatar/" + username + "/" + this.imageSize + ".png");
        this.downloadedImages.put(username, imageData);
    }
}
