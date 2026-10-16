package vegas.sony.friend;

import com.google.gson.JsonObject;
import vegas.sony.friend.Friend;
import vegas.sony.friend.FriendEntry;
import vegas.sony.friend.OnlineFriend;

public class ExternalFriend
extends FriendEntry {
    private final OnlineFriend onlineFriend;


    @Override
    public JsonObject toJson() {
        return null;
    }

    @Override
    public String getName() {
        if (this.onlineFriend.isVisible()) {
            return this.onlineFriend.getMinecraftUsername();
        }
        return "";
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    public OnlineFriend getOnlineFriend() {
        return this.onlineFriend;
    }

    @Override
    public Friend loadJson(JsonObject jsonObject) {
        return null;
    }

    public ExternalFriend(OnlineFriend onlineFriend) {
        this.onlineFriend = onlineFriend;
    }
}

