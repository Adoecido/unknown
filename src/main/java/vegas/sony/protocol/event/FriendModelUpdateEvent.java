package vegas.sony.protocol.event;

import vegas.sony.friend.FriendModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEventPayloadBase;

public class FriendModelUpdateEvent
extends OnlineEventPayloadBase {
    private final FriendModel P;

    public FriendModelUpdateEvent(ZeusClient zeusClient, FriendModel friendModel) {
        super(zeusClient, friendModel.getUser());
        this.P = friendModel;
    }

    public FriendModel q() {
        return this.P;
    }
}
