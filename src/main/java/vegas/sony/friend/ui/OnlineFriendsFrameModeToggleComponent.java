package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendsFrame;
import vegas.sony.friend.ui.OnlineModeToggleComponent;

public class OnlineFriendsFrameModeToggleComponent
extends OnlineModeToggleComponent {
    private final OnlineFriendsFrame friendsFrame;

    public OnlineFriendsFrameModeToggleComponent(OnlineFriendsFrame onlineFriendsFrame, String string, String string2, boolean bl) {
        super(string, string2, bl);
        this.friendsFrame = onlineFriendsFrame;
    }

    @Override
    public void setLeftSelected(Boolean selected) {
        super.setLeftSelected(selected);
        this.friendsFrame.updateFriendManagementPopup(selected);
    }
}
