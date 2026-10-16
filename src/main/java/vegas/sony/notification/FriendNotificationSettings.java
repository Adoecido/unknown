package vegas.sony.notification;

import vegas.sony.value.BooleanValue;

public class FriendNotificationSettings {
    public final BooleanValue friendOnline;
    public final BooleanValue partyInviteAccepted;
    public final BooleanValue partyInvites;
    public final BooleanValue chats;
    public final BooleanValue friendRequests;
    public final BooleanValue general = BooleanValue.create(null, "Too many pings //$ui", true);

    public FriendNotificationSettings() {
        this.friendRequests = BooleanValue.create(null, "Friend requests //$ui", true);
        this.chats = BooleanValue.create(null, "Chats //$ui", true);
        this.friendOnline = BooleanValue.create(null, "Friend online //$ui", true);
        this.partyInvites = BooleanValue.create(null, "Party invites //$ui", true);
        this.partyInviteAccepted = BooleanValue.create(null, "Party invite accepted //$ui", true);
    }
}
