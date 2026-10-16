package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.utils.ThreadBoundExecutor;
import vegas.sony.wrapper.impl.DeltaTracker;

public class EventRenderWorldPassExecutorDrain
extends Event {
    private static final EventListeners EVENT_LISTENERS;
    private final float partialTicks;
    public static final ThreadBoundExecutor EXECUTOR;
    private static String[] obfuscationState;
    private static int drainFireLogCount;

    public EventRenderWorldPassExecutorDrain(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public static String[] getWorldPassObfuscationState() {
        return obfuscationState;
    }

    @Override
    public boolean fire() {
        try {
            if (drainFireLogCount < 3) {
                ++drainFireLogCount;
                vegas.sony.runtime.CheatBreakerDebugLog.info("EVENT_DRAIN_FIRED n=" + drainFireLogCount + " //$");
            }
            EXECUTOR.runPending();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    public static void setWorldPassObfuscationState(String[] state) {
        obfuscationState = state;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public EventRenderWorldPassExecutorDrain(Object deltaTrackerHandle) {
        DeltaTracker deltaTracker = new DeltaTracker(deltaTrackerHandle);
        this.partialTicks = deltaTracker.getGameTimeDeltaTicks();
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    static {
        EXECUTOR = new ThreadBoundExecutor();
        EVENT_LISTENERS = new EventListeners();
        EventRenderWorldPassExecutorDrain.setWorldPassObfuscationState(null);
    }
}

