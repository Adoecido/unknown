package vegas.sony.event.listener;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventWorldChange;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.WorldClient;

public class WorldChangeEventDispatcher
implements EventListener {
    private WorldClient currentWorld;


    @EventHandler(priority=EventPriority.LOW)
    public void onTick(EventPreTick eventTick) {
        WorldClient world = eventTick.getWorld();
        boolean worldUnavailable = world.isNull();
        if (this.currentWorld == null && !worldUnavailable) {
            this.logTransition("null", world);
            new EventWorldChange(this.currentWorld, world).fire();
            this.currentWorld = world;
        } else if (this.currentWorld != null && !worldUnavailable && this.currentWorld.getObject() != world.getObject()) {
            this.logTransition("world", world);
            new EventWorldChange(this.currentWorld, world).fire();
            this.currentWorld = world;
        } else if (this.currentWorld != null && worldUnavailable) {
            this.logTransition("world", null);
            new EventWorldChange(this.currentWorld, null).fire();
            this.currentWorld = null;
        }
    }

    /**
     * Badlion 1.8.9 bring-up trace: server -&gt; singleplayer killed binds in
     * testing, so record every world transition. Badlion-only; Lunar/CB logs
     * are untouched.
     */
    private void logTransition(String from, WorldClient to) {
        try {
            if (!NativeBridge.isBadlion189Runtime()) {
                return;
            }
            // EVENT_ prefix: CheatBreakerDebugLog.info drops other prefixes.
            CheatBreakerDebugLog.info("EVENT_WORLD_CHANGE from=" + from
                    + " toNull=" + (to == null || to.isNull())
                    + " thread=" + Thread.currentThread().getName() + " //$");
        } catch (Throwable ignored) {
            // Transition logging must never break tick dispatch.
        }
    }
}

