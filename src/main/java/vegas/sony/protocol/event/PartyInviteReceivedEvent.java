package vegas.sony.protocol.event;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class PartyInviteReceivedEvent
extends OnlineEvent {
    private final UserModel R;

    public UserModel R() {
        return this.R;
    }

    public PartyInviteReceivedEvent(ZeusClient zeusClient, UserModel userModel) {
        super(zeusClient);
        this.R = userModel;
    }
}

