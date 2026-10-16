package vegas.sony.sync;

import vegas.sony.SonyVegas;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncStoreRequestWorker
implements Runnable {
    private final AtomicBoolean saveRequested = new AtomicBoolean();

    public void requestSave() {
        this.saveRequested.set(true);
    }

    @Override
    public void run() {
        while (!SonyVegas.INSTANCE.isEnabled()) {
            try {
                Thread.sleep(100L);
            }
            catch (InterruptedException interruptedException) {
                {}
            }
            if (!this.saveRequested.get()) continue;
            try {
                Thread.sleep(100L);
            }
            catch (InterruptedException interruptedException) {
                {}
            }
            SonyVegas.INSTANCE.getSyncThread().saveSettings();
            this.saveRequested.set(false);
        }
    }
}

