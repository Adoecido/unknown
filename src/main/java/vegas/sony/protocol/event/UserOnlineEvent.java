package vegas.sony.protocol.event;

import vegas.sony.friend.FriendRequestModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public abstract class UserOnlineEvent
extends OnlineEvent {
    private final FriendRequestModel G;
    private static boolean y;

    public UserOnlineEvent(ZeusClient oZ, FriendRequestModel friendRequestModel) {
        super(oZ);
        this.G = friendRequestModel;
    }


    public static boolean M() {
        return y;
    }

    public static void L(boolean bl) {
        y = bl;
    }

    public static boolean I() {
        boolean bl = UserOnlineEvent.M();
        return true;
    }

    public FriendRequestModel q() {
        return this.G;
    }

    static {
        if (UserOnlineEvent.M()) {
            UserOnlineEvent.L(true);
        }
    }
}

