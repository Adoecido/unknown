package vegas.sony.protocol.event;

import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class FriendRequestRemovedEvent
extends OnlineEvent {
    private final long g;

    public long v() {
        return this.g;
    }

    public FriendRequestRemovedEvent(ZeusClient oZ, long l) {
        super(oZ);
        this.g = l;
    }
}

