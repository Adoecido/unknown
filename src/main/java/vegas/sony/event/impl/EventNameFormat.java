package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.ITextComponent;

public class EventNameFormat
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private ITextComponent displayName;
    private final EntityPlayer player;

    public EntityPlayer getPlayer() {
        return this.player;
    }

    public EventNameFormat(EntityPlayer player, ITextComponent displayName) {
        this.player = player;
        this.displayName = displayName;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public ITextComponent getDisplayName() {
        return this.displayName;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    public void setDisplayName(ITextComponent displayName) {
        this.displayName = displayName;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }
}
