package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.ResourceLocationKey;
import vegas.sony.wrapper.impl.ResourceLocationName;

public class EventPreRenderLivingSpecials
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final Object resourceLocationHandle;

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public EventPreRenderLivingSpecials(Object resourceLocationHandle) {
        this.resourceLocationHandle = resourceLocationHandle;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        try {
            ResourceLocationName resourceLocationName = new ResourceLocationName(this.resourceLocationHandle);
            String resourceName = ForgeVersion.c() == ForgeVersion.MC_1_8_9.i()
                    ? resourceLocationName.getCompleteReport()
                    : resourceLocationName.getFriendlyReport(ResourceLocationKey.L());
            SonyVegas.logError(resourceName);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return super.fire();
    }
}
