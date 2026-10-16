package vegas.sony.friend;

import vegas.sony.friend.OnlineFriend;

public class PartyInvite {
    private final OnlineFriend inviter;

    public PartyInvite(OnlineFriend inviter) {
        this.inviter = inviter;
    }

    public OnlineFriend getInviter() {
        return this.inviter;
    }
}
