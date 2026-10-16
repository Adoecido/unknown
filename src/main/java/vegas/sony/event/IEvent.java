package vegas.sony.event;

import vegas.sony.event.EventBus;
import vegas.sony.event.EventListeners;

public interface IEvent {
    public EventListeners getListeners();

    default public boolean fire() {
        EventBus.getInstance().post(this);
        return true;
    }
}

