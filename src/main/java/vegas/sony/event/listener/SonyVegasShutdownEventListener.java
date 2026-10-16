package vegas.sony.event.listener;

import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.UseEntityPacketBridge;

public class SonyVegasShutdownEventListener
implements EventListener {

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPacketSend(EventPacketSend eventPacketSend) {
        UseEntityPacketBridge useEntityPacketBridge;
        int entityId;
        Packet packet = eventPacketSend.getPacket();
        if (UseEntityPacketBridge.isUseEntityPacket(packet) && ClientSettings.isReservedEntityId(entityId = (useEntityPacketBridge = new UseEntityPacketBridge(packet.getObject())).getEntityId())) {
            eventPacketSend.setCancelled(true);
        }
    }
}

