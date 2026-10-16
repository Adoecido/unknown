package vegas.sony.event.impl;

import vegas.sony.config.Profile;
import vegas.sony.event.EventListeners;
import vegas.sony.event.IEvent;
import vegas.sony.event.impl.ProfileListMutationAction;

public class ProfileListMutationEvent
implements IEvent {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final ProfileListMutationAction action;
    private final Profile profile;

    public ProfileListMutationEvent(Profile profile, ProfileListMutationAction action) {
        this.profile = profile;
        this.action = action;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public Profile getProfile() {
        return this.profile;
    }

    public ProfileListMutationAction getAction() {
        return this.action;
    }
}
