package vegas.sony.event.impl;

import vegas.sony.config.PublicProfile;
import vegas.sony.event.EventListeners;
import vegas.sony.event.IEvent;

public class PublicProfileCreatedEvent
implements IEvent {
    private final PublicProfile profile;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public PublicProfile getProfile() {
        return this.profile;
    }

    public PublicProfileCreatedEvent(PublicProfile profile) {
        this.profile = profile;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }
}
