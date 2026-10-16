package vegas.sony.movement;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPreLocalPlayerTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.wrapper.impl.Minecraft;

public class PlayerMovementTaskManager
implements EventListener {
    private static String controlFlowMarker;
    public static PlayerMovementTaskManager INSTANCE;
    private PlayerMovementTask activeTask;

    private static RuntimeException propagateRuntimeException(RuntimeException exception) {
        return exception;
    }

    public static void setControlFlowMarker(String marker) {
        controlFlowMarker = marker;
    }

    public void cancel(PlayerMovementTask task) {
        if (this.activeTask != null && this.activeTask.equals(task)) {
            this.activeTask.setCompleted(true);
        }
    }

    private PlayerMovementTaskManager() {
        INSTANCE = this;
    }

    public boolean e$src$Z$17ayaq9() {
        return this.activeTask != null && !this.activeTask.isCompleted();
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onPreLocalPlayerTick(EventPreLocalPlayerTick event) {
        if (this.activeTask == null && Minecraft.thePlayer().isNotNull()) {
            return;
        }
        this.activeTask.updateCompletion();
        if (this.activeTask.isCompleted()) {
            if (this.activeTask.shouldRestoreInputOnCompletion()) {
                MovementInputHelper.restorePhysicalInput();
            } else {
                MovementInputHelper.releaseMovementKeys();
            }
            this.activeTask = null;
        }
    }

    public PlayerMovementTask getActiveTask() {
        return this.activeTask;
    }

    public static String getControlFlowMarker() {
        return controlFlowMarker;
    }

    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        if (this.activeTask != null && Minecraft.thePlayer().isNotNull()) {
            try {
                this.activeTask.applyMovementInput(eventPreTick);
            }
            catch (NullPointerException nullPointerException) {
                SonyVegas.logThrowable(nullPointerException);
            }
        }
    }

    public void submit(PlayerMovementTask task) {
        this.activeTask = task;
    }

    static {
        INSTANCE = new PlayerMovementTaskManager();
        PlayerMovementTaskManager.setControlFlowMarker(null);
    }
}
