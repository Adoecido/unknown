package vegas.sony.input;

import vegas.sony.input.BindCaptureThread;
import vegas.sony.unmap.Bendable;

public abstract class BindCaptureTask
implements Runnable {
    private static volatile boolean anyCaptureActive;
    private BindCaptureThread captureThread;
    private Bendable bendable;

    @Override
    public void run() {
        if (this.captureThread == null) {
            anyCaptureActive = true;
            this.captureThread = new BindCaptureThread(this, null);
            this.captureThread.start();
        }
    }

    public BindCaptureTask(Bendable bendable) {
        this.bendable = bendable;
    }

    public void setBendable(Bendable bendable) {
        this.bendable = bendable;
    }

    public boolean isCapturing() {
        return this.captureThread != null;
    }

    public final void finishCapture() {
        this.captureThread = null;
        anyCaptureActive = false;
        this.onCaptureComplete();
    }

    public static boolean isAnyCaptureActive() {
        return anyCaptureActive;
    }

    Bendable getBendable() {
        return this.bendable;
    }

    public abstract void onCaptureComplete();
}

