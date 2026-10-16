package vegas.sony.protocol.event;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEventPayloadBase;

public class FriendRemovedEvent
extends OnlineEventPayloadBase {
    public FriendRemovedEvent(ZeusClient zeusClient, UserModel userModel) {
        super(zeusClient, userModel);
    }
}

