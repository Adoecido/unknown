package vegas.sony.friend;

import vegas.sony.SonyVegas;
import vegas.sony.friend.FriendRequestModel;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.UserModel;

public class FriendRequest {
    private final OnlineFriend friend;
    private static String[] obfuscationState;
    private final long id;

    public FriendRequest(FriendRequestModel model, boolean incoming) {
        this.id = model.getId();
        UserModel remoteUser = incoming ? model.getRequester() : model.getRecipient();
        this.friend = SonyVegas.INSTANCE.getOnlineManager().getFriendCache().getOrCreateFriend(remoteUser.getId(), () -> new OnlineFriend(remoteUser));
    }

    static {
        if (FriendRequest.getObfuscationState() != null) {
            FriendRequest.setObfuscationState(new String[4]);
        }
    }

    public static void setObfuscationState(String[] state) {
        obfuscationState = state;
    }

    public long getId() {
        return this.id;
    }

    public static String[] getObfuscationState() {
        return obfuscationState;
    }

    public OnlineFriend getFriend() {
        return this.friend;
    }
}
