package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GlStateManager;
import vegas.sony.wrapper.impl.RenderSystem;

public class EventFogDensity
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private float density;

    public void setDensity(float density) {
        this.density = density;
    }


    @Override
    public boolean fire() {
        boolean applied = super.fire();
        if (applied) {
            if (ForgeVersion.MC_1_8_9.B()) {
                GlStateManager.g(this.density);
            } else if (ForgeVersion.MC_1_16_5.d()) {
                RenderSystem.o(this.density);
            }
        }
        return applied;
    }

    public EventFogDensity(float density) {
        this.density = density;
    }

    public float getDensity() {
        return this.density;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }
}

