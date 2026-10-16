package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.ItemStack;

public class EventPlayerUseItem
extends Event {
    private final ItemStack itemStack;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public EventPlayerUseItem(Object itemStackHandle) {
        this.itemStack = new ItemStack(itemStackHandle);
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }
}
