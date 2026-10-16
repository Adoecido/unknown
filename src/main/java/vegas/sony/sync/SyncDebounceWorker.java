package vegas.sony.sync;

import vegas.sony.SonyVegas;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.utils.SleepUtil;

public class SyncDebounceWorker
implements Runnable {
    private static final long DEBOUNCE_MILLIS = 3000L;
    private long lastChangeTime;

    @Override
    public void run() {
        while (!SonyVegas.INSTANCE.isEnabled()) {
            this.processPendingSave();
        }
    }

    public void markChanged() {
        this.lastChangeTime = System.currentTimeMillis();
    }

    private void processPendingSave() {
        try {
            SleepUtil.sleep(1000L);
            if (!SonyVegas.INSTANCE.getPublicProfileSettings().autoSave.getEffectiveValue()) {
                return;
            }
            if (!ClientSettings.INSTANCE.isMainGuiStack() && !ClientSettings.INSTANCE.inputEnabled) {
                return;
            }
            long observedChangeTime = this.lastChangeTime;
            if (SonyVegas.INSTANCE.getSyncThread().hasPendingSave()) {
                SleepUtil.sleep(DEBOUNCE_MILLIS);
                if (this.lastChangeTime == observedChangeTime) {
                    SonyVegas.INSTANCE.getSyncThread().requestSave();
                }
            }
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }
}
