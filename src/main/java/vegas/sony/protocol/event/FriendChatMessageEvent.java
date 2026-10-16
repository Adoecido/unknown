package vegas.sony.protocol.event;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class FriendChatMessageEvent
extends OnlineEvent {
    private final String t;
    private final UserModel q;

    public UserModel U() {
        return this.q;
    }

    public String g() {
        return this.t;
    }

    public FriendChatMessageEvent(ZeusClient oZ, UserModel oj_12, String string) {
        super(oZ);
        this.q = oj_12;
        this.t = string;
    }
}

