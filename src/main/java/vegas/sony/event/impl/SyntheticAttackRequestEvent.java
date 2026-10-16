package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.module.Mod;

public class SyntheticAttackRequestEvent
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final Mod source;
    private final boolean followedByMouseInput;

    public Mod getSource() {
        return this.source;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public SyntheticAttackRequestEvent(Mod mod) {
        this(mod, false);
    }

    public SyntheticAttackRequestEvent(Mod mod, boolean followedByMouseInput) {
        this.source = mod;
        this.followedByMouseInput = followedByMouseInput;
    }

    public boolean isFollowedByMouseInput() {
        return this.followedByMouseInput;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }
}
