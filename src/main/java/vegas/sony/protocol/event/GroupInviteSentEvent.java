package vegas.sony.protocol.event;

import vegas.sony.friend.OnlineFriend;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class GroupInviteSentEvent
extends OnlineEvent {
    private final OnlineFriend I;

    public GroupInviteSentEvent(ZeusClient oZ, OnlineFriend yS) {
        super(oZ);
        this.I = yS;
    }

    public OnlineFriend n() {
        return this.I;
    }
}

