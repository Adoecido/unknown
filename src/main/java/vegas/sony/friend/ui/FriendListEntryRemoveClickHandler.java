package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.ExternalFriend;
import vegas.sony.friend.Friend;
import vegas.sony.friend.FriendEntry;
import vegas.sony.friend.ui.FriendListEntryRow;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.ui.click.component.GuiClickListener;

public class FriendListEntryRemoveClickHandler
implements GuiClickListener {
    private final FriendEntry friendEntry;
    private final FriendListEntryRow entryRow;

    public FriendListEntryRemoveClickHandler(FriendListEntryRow friendListEntryRow, FriendEntry friendEntry) {
        this.entryRow = friendListEntryRow;
        this.friendEntry = friendEntry;
    }


    @Override
    public void onPrimaryClick() {
        if (this.friendEntry instanceof Friend) {
            SonyVegas.INSTANCE.getFriendManager().removeFriend((Friend)this.friendEntry);
        } else if (this.friendEntry instanceof ExternalFriend) {
            ExternalFriend externalFriend = (ExternalFriend)this.friendEntry;
            externalFriend.getOnlineFriend().setSyncWithFriends(false);
        }
        OnlineFriendUiHelper.refreshFriendLists();
    }
}

