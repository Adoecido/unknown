package vegas.sony.protocol.event;

import vegas.sony.friend.PartyState;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class GroupCreatedEvent
extends OnlineEvent {
    private final PartyState Y;

    public GroupCreatedEvent(ZeusClient oZ, PartyState wF) {
        super(oZ);
        this.Y = wF;
    }

    public PartyState V() {
        return this.Y;
    }
}

