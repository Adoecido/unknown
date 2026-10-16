package vegas.sony.event.listener;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.PlayerEventNameFormat;

public class SonyVegasLifecycleEventListener
implements EventListener {

    @EventHandler
    public void onPacketReceive(EventPacketReceive eventPacketReceive) {
        PlayerEventNameFormat resourcePackPacket;
        String resourcePackUrl;
        if (eventPacketReceive.getPacket().isInstance(MappedClasses.l3) && (resourcePackUrl = (resourcePackPacket = new PlayerEventNameFormat(eventPacketReceive.getPacket())).getUrl()).contains("svc")) {
            resourcePackPacket.setUrl(resourcePackUrl.replace("svc", "-"));
        }
    }
}

