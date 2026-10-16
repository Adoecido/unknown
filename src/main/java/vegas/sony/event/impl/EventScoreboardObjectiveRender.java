package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.ScoreObjective;

public class EventScoreboardObjectiveRender
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }


    public EventScoreboardObjectiveRender(Object objectiveOrMatrixStackHandle, Object objectiveOrRenderContextHandle) {
    }

    public EventScoreboardObjectiveRender(Object objectiveHandle, int x, int y, Object scoreboardHandle) {
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }
}

