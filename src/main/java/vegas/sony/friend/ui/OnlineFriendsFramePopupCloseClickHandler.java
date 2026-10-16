package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendsFrame;
import vegas.sony.ui.click.component.GuiClickListener;

public class OnlineFriendsFramePopupCloseClickHandler
implements GuiClickListener {
    private final OnlineFriendsFrame friendsFrame;

    @Override
    public void onPrimaryClick() {
        OnlineFriendsFrame.toggleFriendRequestsPopup(this.friendsFrame);
    }

    public OnlineFriendsFramePopupCloseClickHandler(OnlineFriendsFrame onlineFriendsFrame) {
        this.friendsFrame = onlineFriendsFrame;
    }
}
