package vegas.sony.event.forge;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventChat;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.ClientChatReceivedEvent;
import vegas.sony.wrapper.impl.ForgeVersion;

public class ForgeClientChatReceivedEvent
implements EventListener {

    @EventHandler(priority=EventPriority.LOW)
    public void onPacketReceive(EventPacketReceive eventPacketReceive) {
        if (!eventPacketReceive.getPacket().isInstance(MappedClasses.Zu)) {
            return;
        }
        if (ForgeVersion.MC_1_20_6.d()) {
            return;
        }
        ClientChatReceivedEvent clientChatReceivedEvent = new ClientChatReceivedEvent(eventPacketReceive.getPacket().getObject());
        EventChat eventChat = new EventChat(clientChatReceivedEvent.P());
        eventChat.fire();
        if (eventChat.isCanceled()) {
            eventPacketReceive.setCancelled(true);
        } else {
            clientChatReceivedEvent.t(eventChat.getMessage());
        }
    }
}

