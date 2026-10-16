package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendsFrame;
import vegas.sony.ui.click.component.GuiClickListener;

public class OnlineFriendsFrameConditionalPopupCloseClickHandler
implements GuiClickListener {
    private final OnlineFriendsFrame friendsFrame;

    @Override
    public void onPrimaryClick() {
        if (OnlineFriendsFrame.getFriendRequestsPopup(this.friendsFrame) != null) {
            OnlineFriendsFrame.toggleFriendRequestsPopup(this.friendsFrame);
        }
    }


    public OnlineFriendsFrameConditionalPopupCloseClickHandler(OnlineFriendsFrame onlineFriendsFrame) {
        this.friendsFrame = onlineFriendsFrame;
    }
}

