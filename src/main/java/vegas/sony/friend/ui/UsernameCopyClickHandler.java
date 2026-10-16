package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.friend.ui.UsernameEditorPanel;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.notification.NotificationMessage;
import vegas.sony.ui.notification.NotificationType;
import vegas.sony.utils.ClipboardUtil;

public class UsernameCopyClickHandler
implements GuiClickListener {
    private final UsernameEditorPanel editorPanel;

    @Override
    public void onPrimaryClick() {
        ClipboardUtil.setText(SonyVegas.INSTANCE.getOnlineManager().getLocalFriend().getDisplayName());
        OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.SUCCESS, "Copied " + SonyVegas.INSTANCE.getOnlineManager().getLocalFriend().getDisplayName() + " to clipboard"));
    }

    public UsernameCopyClickHandler(UsernameEditorPanel usernameEditorPanel) {
        this.editorPanel = usernameEditorPanel;
    }
}
