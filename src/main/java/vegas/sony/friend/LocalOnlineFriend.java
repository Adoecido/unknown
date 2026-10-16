package vegas.sony.friend;

import vegas.sony.account.MinecraftSessionWrapper;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.OnlineFriendActivityState;
import vegas.sony.friend.UserModel;
import vegas.sony.friend.ui.OnlineFriendActivityPanel;
import vegas.sony.wrapper.impl.Minecraft;

public class LocalOnlineFriend
extends OnlineFriend {
    private final OnlineFriendActivityState activityState;
    private static final String DEFAULT_DISPLAY_NAME = "Self#1234";
    private final OnlineFriendActivityPanel activityPanel;

    public void setUser(UserModel userModel) {
        this.user = userModel;
    }

    public OnlineFriendActivityState getActivityState() {
        return this.activityState;
    }

    public OnlineFriendActivityPanel getActivityPanel() {
        return this.activityPanel;
    }

    public LocalOnlineFriend() {
        super(DEFAULT_DISPLAY_NAME);
        MinecraftSessionWrapper minecraftSessionWrapper = Minecraft.Q$src$Lvegas_sony_account_MinecraftSessionWrapper_$1ftnn3u();
        this.updateMinecraftProfile(minecraftSessionWrapper.getProfileId(), minecraftSessionWrapper.getUsername());
        this.activityState = new OnlineFriendActivityState(this);
        this.activityPanel = new OnlineFriendActivityPanel(this);
    }
}
