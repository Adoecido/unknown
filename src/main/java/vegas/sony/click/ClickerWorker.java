package vegas.sony.click;

import vegas.sony.SonyVegas;
import vegas.sony.module.combat.ClickerMod;
import vegas.sony.utils.SleepUtil;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClickerWorker
implements Runnable {
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final Object activityMonitor = new Object();
    private boolean started;
    private final ClickerMod owner;

    public ClickerWorker(ClickerMod owner) {
        this.owner = owner;
    }

    public synchronized void startOrResume() {
        if (!this.started) {
            this.started = true;
            Thread workerThread = new Thread(this, "Worker");
            workerThread.setDaemon(true);
            workerThread.start();
        }
        this.resume();
    }

    private void awaitActivityAndRunClickCycle() {
        ClickEngine clickEngine = this.owner.getClickEngine();
        try {
            synchronized (this.activityMonitor) {
                while (!this.active.get()) {
                    try {
                        this.activityMonitor.wait();
                    }
                    catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            if (clickEngine.getButton() == ClickButton.LEFT && this.owner.isClickCycleBlocked()) {
                return;
            }
            this.owner.runClickCycle();
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            SleepUtil.sleep(5L);
            this.awaitActivityAndRunClickCycle();
        }
    }

    public void resume() {
        synchronized (this.activityMonitor) {
            this.active.set(true);
            this.activityMonitor.notify();
        }
    }

    public void pause() {
        synchronized (this.activityMonitor) {
            this.active.set(false);
        }
    }
}