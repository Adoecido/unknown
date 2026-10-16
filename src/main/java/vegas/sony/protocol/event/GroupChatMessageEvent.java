package vegas.sony.protocol.event;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEventPayloadBase;

public class GroupChatMessageEvent
extends OnlineEventPayloadBase {
    private final String d;
    private final UserModel h;

    public GroupChatMessageEvent(ZeusClient oZ, UserModel oj_12, UserModel oj_13, String string) {
        super(oZ, oj_12);
        this.h = oj_13;
        this.d = string;
    }

    public String K() {
        return this.d;
    }

    public UserModel V() {
        return this.h;
    }
}

