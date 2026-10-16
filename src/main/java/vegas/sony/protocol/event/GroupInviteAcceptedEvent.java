package vegas.sony.protocol.event;

import vegas.sony.friend.PartyState;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class GroupInviteAcceptedEvent
extends OnlineEvent {
    private final PartyState c;

    public PartyState P() {
        return this.c;
    }

    public GroupInviteAcceptedEvent(ZeusClient oZ, PartyState wF) {
        super(oZ);
        this.c = wF;
    }
}

