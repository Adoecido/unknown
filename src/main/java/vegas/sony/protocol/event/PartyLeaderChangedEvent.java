package vegas.sony.protocol.event;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class PartyLeaderChangedEvent
extends OnlineEvent {
    private final UserModel j;

    public PartyLeaderChangedEvent(ZeusClient oZ, UserModel oj_12) {
        super(oZ);
        this.j = oj_12;
    }

    public UserModel z() {
        return this.j;
    }
}

