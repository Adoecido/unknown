package vegas.sony.protocol.event;

import vegas.sony.friend.FriendRequestModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.UserOnlineEvent;

public class FriendRequestSentEvent
extends UserOnlineEvent {
    public FriendRequestSentEvent(ZeusClient zeusClient, FriendRequestModel friendRequestModel) {
        super(zeusClient, friendRequestModel);
    }
}

