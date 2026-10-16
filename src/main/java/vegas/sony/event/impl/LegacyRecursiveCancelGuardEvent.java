package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;

public class LegacyRecursiveCancelGuardEvent
extends Event {
    private static int[] obfuscationState;
    static int invocationCounter;
    private static final EventListeners EVENT_LISTENERS;

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }


    public static int[] getGuardObfuscationState() {
        return obfuscationState;
    }

    @Override
    public boolean fire() {
        if (invocationCounter > 0) {
            this.setCancelled(true);
        }
        if (++invocationCounter > 100) {
            invocationCounter = 0;
        }
        return super.fire();
    }

    public static void setGuardObfuscationState(int[] state) {
        obfuscationState = state;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    static {
        invocationCounter = 0;
        EVENT_LISTENERS = new EventListeners();
        LegacyRecursiveCancelGuardEvent.setGuardObfuscationState(new int[5]);
    }
}

