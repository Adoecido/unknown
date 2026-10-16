package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.FriendRequest;
import vegas.sony.friend.FriendRequestService;
import vegas.sony.friend.IncomingFriendRequest;
import vegas.sony.friend.ui.FriendRequestListPanel;
import vegas.sony.friend.ui.FriendRequestUsernameInputComponent;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.friend.ui.UsernameEditorPanel;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.TextInputComponentBase;
import vegas.sony.ui.click.component.layout.PaddedComponent;
import vegas.sony.ui.notification.NotificationMessage;
import vegas.sony.ui.notification.NotificationType;

public class FriendRequestsPanel
extends PanelComponent {
    private final FriendRequestListPanel requestListPanel;
    private final TextInputComponentBase usernameInput;
    private final UsernameEditorPanel usernameEditor = new UsernameEditorPanel();

    static void submitUsername(FriendRequestsPanel panel, String username) {
        panel.handleUsernameSubmission(username);
    }

    public FriendRequestListPanel getRequestListPanel() {
        return this.requestListPanel;
    }

    public FriendRequestsPanel() {
        super(105.0, 150.0);
        this.usernameInput = new FriendRequestUsernameInputComponent(this, "Add friend...");
        this.setDisabledOverlayColor(FriendRequestsPanel.J.i);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.usernameInput.setVisible(true);
        this.h(this.usernameEditor, new Object[0]);
        this.h(new SpacerComponent(1.0, 2.0), new Object[0]);
        this.h(this.usernameInput, new Object[0]);
        this.requestListPanel = new FriendRequestListPanel();
        this.h(new PaddedComponent(3.0, this.requestListPanel), new Object[0]);
    }

    private void handleUsernameSubmission(String username) {
        String submittedUsername = username;
        if (submittedUsername.isEmpty()) {
            return;
        }
        for (FriendRequest friendRequest : SonyVegas.INSTANCE.getOnlineManager().getFriendRequestManager().getIncomingRequests()) {
            if (!friendRequest.getFriend().getDisplayName().equals(submittedUsername)) continue;
            OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.SUCCESS, "Added " + submittedUsername + " as a friend"));
            SonyVegas.INSTANCE.getOnlineManager().getFriendRequestManager().acceptIncomingRequest((IncomingFriendRequest)friendRequest);
            return;
        }
        FriendRequestService.sendFriendRequest(submittedUsername);
    }

    @Override
    public void c() {
        super.c();
    }

}

