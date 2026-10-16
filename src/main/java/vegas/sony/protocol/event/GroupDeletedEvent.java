package vegas.sony.protocol.event;

import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class GroupDeletedEvent
extends OnlineEvent {
    public GroupDeletedEvent(ZeusClient oZ) {
        super(oZ);
    }
}

