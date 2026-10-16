package vegas.sony.event.impl;

import vegas.sony.event.EventListeners;
import vegas.sony.event.IEvent;
import java.util.Collection;
import org.jetbrains.annotations.UnmodifiableView;

public class PublicProfileTagsUpdatedEvent
implements IEvent {
    private final Collection<String> tags;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public PublicProfileTagsUpdatedEvent(Collection<String> tags) {
        this.tags = tags;
    }

    public @UnmodifiableView Collection<String> getTags() {
        return this.tags;
    }
}
