package vegas.sony.protocol.event;

import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class UserDisplayNameChangedEvent
extends OnlineEvent {
    private final long s;
    private final String G;

    public UserDisplayNameChangedEvent(ZeusClient zeusClient, long l, String string) {
        super(zeusClient);
        this.s = l;
        this.G = string;
    }

    public long R() {
        return this.s;
    }

    public String v() {
        return this.G;
    }
}

