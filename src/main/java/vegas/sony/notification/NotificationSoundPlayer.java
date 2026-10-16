package vegas.sony.notification;

import vegas.sony.SonyVegas;
import vegas.sony.notification.SoundClip;
import java.util.concurrent.atomic.AtomicReference;

public class NotificationSoundPlayer {
    private static int[] controlFlowMarker;
    private final AtomicReference<SoundClip> pendingSound = new AtomicReference<SoundClip>();

    public NotificationSoundPlayer() {
        this.startSoundThread();
    }

    static {
        if (NotificationSoundPlayer.getControlFlowMarker() == null) {
            NotificationSoundPlayer.setControlFlowMarker(new int[3]);
        }
    }

    public void playPendingSound() {
        if (this.pendingSound.get() != null) {
            SoundClip sound = this.pendingSound.get();
            this.pendingSound.set(null);
            if (!this.isMuted()) {
                sound.play(this.getVolumePercent());
            }
        }
    }

    public boolean isMuted() {
        return SonyVegas.INSTANCE.getPublicProfileSettings().muted.getEffectiveValue()
                || !SonyVegas.INSTANCE.getPublicProfileSettings().notifications.getEffectiveValue();
    }

    public static int[] getControlFlowMarker() {
        return controlFlowMarker;
    }

    public static void setControlFlowMarker(int[] marker) {
        controlFlowMarker = marker;
    }


    public float getVolumePercent() {
        return ((Double)SonyVegas.INSTANCE.getPublicProfileSettings().volume.getValue()).floatValue();
    }

    public void queue(SoundClip sound) {
        this.pendingSound.set(sound);
    }

    public void startSoundThread() {
        new Thread(this::runSoundLoop, "ns").start();
    }

    private void runSoundLoop() {
        while (!SonyVegas.INSTANCE.enabled) {
            try {
                Thread.sleep(100L);
                this.playPendingSound();
            }
            catch (Exception exception) {
                SonyVegas.logThrowable(exception);
            }
        }
    }
}
