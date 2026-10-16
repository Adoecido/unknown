package vegas.sony.friend;

import vegas.sony.friend.FriendRequest;
import vegas.sony.friend.FriendRequestModel;

public class IncomingFriendRequest
extends FriendRequest {
    public IncomingFriendRequest(FriendRequestModel friendRequestModel) {
        super(friendRequestModel, true);
    }
}

