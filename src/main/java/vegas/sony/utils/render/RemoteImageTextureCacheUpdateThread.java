package vegas.sony.utils.render;

import vegas.sony.SonyVegas;
import vegas.sony.utils.SleepUtil;
import vegas.sony.utils.render.RemoteImageTextureCache;
import vegas.sony.utils.render.RemoteImageTextureManager;

class RemoteImageTextureCacheUpdateThread
extends Thread {
    final RemoteImageTextureManager manager;

    @Override
    public void run() {
        while (!SonyVegas.INSTANCE.isEnabled()) {
            SleepUtil.sleep(50L);
            for (Integer imageSize : RemoteImageTextureManager.getCaches(this.manager).keySet()) {
                ((RemoteImageTextureCache)RemoteImageTextureManager.getCaches(this.manager).get(imageSize)).processPendingDownloads();
            }
        }
    }

    RemoteImageTextureCacheUpdateThread(RemoteImageTextureManager manager) {
        this.manager = manager;
    }
}
