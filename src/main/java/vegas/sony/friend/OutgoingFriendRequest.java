package vegas.sony.friend;

import vegas.sony.friend.FriendRequest;
import vegas.sony.friend.FriendRequestModel;

public class OutgoingFriendRequest
extends FriendRequest {
    public OutgoingFriendRequest(FriendRequestModel friendRequestModel) {
        super(friendRequestModel, false);
    }
}

