package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.DeltaTracker;
import vegas.sony.wrapper.impl.Minecraft;

public class EventRenderTickBase
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final float partialTicks;
    private static String obfuscationState;

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    EventRenderTickBase(float partialTicks) {
        this.partialTicks = partialTicks == -1.0f ? Minecraft.getTimer().renderPartialTicks() : partialTicks;
    }

    public float getTicks() {
        return this.partialTicks;
    }

    public static String getObfuscationState() {
        return obfuscationState;
    }

    EventRenderTickBase(DeltaTracker deltaTracker) {
        this.partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
    }

    public static void setObfuscationState(String state) {
        obfuscationState = state;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    static {
        EventRenderTickBase.setObfuscationState("qQGv3b");
    }
}
