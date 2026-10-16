package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.OnlineStatus;
import vegas.sony.friend.PartyState;
import vegas.sony.friend.ui.OnlineFriendActionIconButton;
import vegas.sony.friend.ui.OnlineFriendActionPanelPopupOutsideClickFilter;
import vegas.sony.friend.ui.OnlineFriendNotificationsValue;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.FriendDeleteResponsePacket;
import vegas.sony.protocol.packet.GroupCreateResponsePacket;
import vegas.sony.protocol.packet.GroupCreateStatus;
import vegas.sony.protocol.packet.GroupInviteResponsePacket;
import vegas.sony.ui.click.animation.ColorAnimation;
import vegas.sony.ui.click.component.ConfirmationDialogComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.gui.IconActionButton;
import vegas.sony.ui.click.component.gui.TextActionButton;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import vegas.sony.ui.click.frame.DimmedCenteredPopupFrame;
import vegas.sony.ui.click.frame.PopupFrame;
import vegas.sony.ui.notification.NotificationMessage;
import vegas.sony.ui.notification.NotificationType;
import vegas.sony.value.BooleanValue;
import java.awt.Color;

public class OnlineFriendActionPanel
extends PanelComponent {
    private final OnlineFriend friend;
    private final TextActionButton chatButton;
    private final BooleanValue syncWithFriendsValue;
    private final BooleanToggleComponent syncWithFriendsToggle;
    private boolean actionPending;
    private final IconActionButton removeButton;
    private final IconActionButton inviteButton;
    private boolean inviteDisabled;

    @Override
    public double C() {
        return 35.0;
    }

    private void handlePartyCreationFailure() {
        this.actionPending = false;
    }

    private void cancelFriendRemoval(PopupFrame popupFrame) {
        ClientSettings.removePopup(popupFrame);
        this.actionPending = false;
    }

    private void showInviteResult(OnlineFriend friend, GroupInviteResponsePacket response) {
        switch (response.getStatus()) {
            case SUCCESS: {
                OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.SUCCESS, "Invited " + friend.getDisplayName() + " to party"));
                break;
            }
            case TOO_MANY_INVITES: {
                OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.ERROR, "Sent too many invites"));
                break;
            }
            case NOT_ONLINE: 
            case ALREADY_INVITED: 
            case FAILED: {
                OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.ERROR, "Error inviting " + friend.getDisplayName() + " to party"));
            }
        }
    }

    private void confirmFriendRemoval(PopupFrame popupFrame) {
        ClientSettings.removePopup(popupFrame);
        ZeusConnectionManager.T().u().i(this.friend.getUser(), this::handleFriendRemovalResponse, this::handleFriendRemovalFailure);
    }

    static boolean isInviteDisabled(OnlineFriendActionPanel panel) {
        return panel.inviteDisabled;
    }


    private void handleNewPartyInviteFailure() {
        this.actionPending = false;
    }

    public OnlineFriendActionPanel(OnlineFriend onlineFriend) {
        super(99.0, 35.0);
        boolean bl;
        this.removeButton = new IconActionButton("newtrash", 0.2, 20.0, 13.0, OnlineFriendActionPanel.J.d, 0.9);
        this.inviteButton = new OnlineFriendActionIconButton(this, "party@2x", 0.2, 20.0, 13.0, OnlineFriendActionPanel.J.B, 0.9);
        this.chatButton = new TextActionButton("CHAT", 0.7, false, 46.0, 13.0, OnlineFriendActionPanel.J.B, 0.9);
        this.syncWithFriendsValue = new OnlineFriendNotificationsValue(this, (Object)null, "Sync with Friends", false);
        this.syncWithFriendsToggle = new BooleanToggleComponent("Sync with friends", 0.8, this.syncWithFriendsValue);
        this.actionPending = false;
        this.friend = onlineFriend;
        boolean bl2 = bl = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty() != null;
        if (bl) {
            boolean bl3 = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty().getMembers().contains(onlineFriend);
            boolean bl4 = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty().getInvitedUsers().contains(onlineFriend);
            this.inviteDisabled = onlineFriend.getStatus().equals((Object)OnlineStatus.OFFLINE) || bl3 || bl4;
            this.inviteButton.setBackgroundAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.m : new Color(45, 45, 45), this.inviteDisabled ? OnlineFriendActionPanel.J.m : this.inviteButton.getBackgroundColor()));
            this.inviteButton.setIconColorAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.W, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.f));
            this.syncWithFriendsValue.setValue(onlineFriend.isSyncWithFriends());
            this.setShowDisabledOverlay(false);
            this.syncWithFriendsToggle.setUseExplicitWidth(true);
            this.syncWithFriendsToggle.o(90.0);
            this.syncWithFriendsToggle.setExplicitWidth(90.0);
            this.syncWithFriendsToggle.setShowDisabledOverlay(false);
            this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap, widthwrap");
            this.removeButton.w("Remove friend");
            this.inviteButton.w("Invite to party");
            this.syncWithFriendsToggle.w("Automatically add friend to Minecraft Friends list");
            this.configureRemoveButton();
            this.inviteButton.addClickListener(() -> this.sendPartyInvite(onlineFriend));
            this.addChildren(new SpacerComponent(99.0, 1.0), new SpacerComponent(6.0, 1.0), this.removeButton, new SpacerComponent(2.0, 1.0), this.inviteButton, new SpacerComponent(2.0, 1.0), this.chatButton, new SpacerComponent(99.0, 2.0), new SpacerComponent(2.0, 1.0), this.syncWithFriendsToggle);
            return;
        }
        boolean bl5 = false;
        boolean bl6 = false;
        this.inviteDisabled = onlineFriend.getStatus().equals((Object)OnlineStatus.OFFLINE);
        this.inviteButton.setBackgroundAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.m : new Color(45, 45, 45), this.inviteDisabled ? OnlineFriendActionPanel.J.m : this.inviteButton.getBackgroundColor()));
        this.inviteButton.setIconColorAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.W, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.f));
        this.syncWithFriendsValue.setValue(onlineFriend.isSyncWithFriends());
        this.setShowDisabledOverlay(false);
        this.syncWithFriendsToggle.setUseExplicitWidth(true);
        this.syncWithFriendsToggle.o(90.0);
        this.syncWithFriendsToggle.setExplicitWidth(90.0);
        this.syncWithFriendsToggle.setShowDisabledOverlay(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap, widthwrap");
        this.removeButton.w("Remove friend");
        this.inviteButton.w("Invite to party");
        this.syncWithFriendsToggle.w("Automatically add friend to Minecraft Friends list");
        this.configureRemoveButton();
        this.inviteButton.addClickListener(() -> this.sendPartyInvite(onlineFriend));
        this.addChildren(new SpacerComponent(99.0, 1.0), new SpacerComponent(6.0, 1.0), this.removeButton, new SpacerComponent(2.0, 1.0), this.inviteButton, new SpacerComponent(2.0, 1.0), this.chatButton, new SpacerComponent(99.0, 2.0), new SpacerComponent(2.0, 1.0), this.syncWithFriendsToggle);
    }

    static OnlineFriend getFriend(OnlineFriendActionPanel panel) {
        return panel.friend;
    }

    private void handleExistingPartyInviteFailure() {
        this.actionPending = false;
    }

    @Override
    public void u() {
        boolean bl;
        boolean bl2;
        super.u();
        boolean bl3 = bl2 = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty() != null;
        if (bl2) {
            boolean bl4;
            boolean bl5 = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty().getMembers().contains(this.friend);
            boolean bl6 = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty().getInvitedUsers().contains(this.friend);
            boolean bl7 = bl4 = this.friend.getStatus().equals((Object)OnlineStatus.OFFLINE) || bl5 || bl6;
            if (this.inviteDisabled != bl4) {
                this.inviteDisabled = bl4;
                this.inviteButton.setBackgroundAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.m : new Color(45, 45, 45), this.inviteDisabled ? OnlineFriendActionPanel.J.m : this.inviteButton.getBackgroundColor()));
                this.inviteButton.setIconColorAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.W, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.f));
            }
            return;
        }
        boolean bl8 = false;
        boolean bl9 = false;
        boolean bl10 = bl = this.friend.getStatus().equals((Object)OnlineStatus.OFFLINE);
        if (this.inviteDisabled != bl) {
            this.inviteDisabled = bl;
            this.inviteButton.setBackgroundAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.m : new Color(45, 45, 45), this.inviteDisabled ? OnlineFriendActionPanel.J.m : this.inviteButton.getBackgroundColor()));
            this.inviteButton.setIconColorAnimation(new ColorAnimation(0.15, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.W, this.inviteDisabled ? OnlineFriendActionPanel.J.K : OnlineFriendActionPanel.J.f));
        }
    }

    private void handleExistingPartyInviteResponse(OnlineFriend friend, GroupInviteResponsePacket response) {
        this.showInviteResult(friend, response);
    }

    private void sendPartyInvite(OnlineFriend friend) {
        if (this.actionPending) {
            return;
        }
        if (friend.getStatus() == OnlineStatus.OFFLINE) {
            return;
        }
        this.actionPending = true;
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState != null) {
            if (!partyState.canInvite()) {
                this.actionPending = false;
                return;
            }
            if (partyState.getMembers().contains(friend)) {
                this.actionPending = false;
                OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.ERROR, "Cannot send party invite to a party member"));
                return;
            }
            if (partyState.getInvitedUsers().contains(friend)) {
                this.actionPending = false;
                OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.ERROR, "Cannot send party invite to an already invited person"));
                return;
            }
            ZeusConnectionManager.T().u().J(friend.getUser(), response -> this.handleExistingPartyInviteResponse(friend, response), this::handleExistingPartyInviteFailure);
            return;
        }
        ZeusConnectionManager.T().u().w(response -> this.handlePartyCreationResponse(friend, response), this::handlePartyCreationFailure);
    }

    private void handleFriendRemovalFailure() {
        this.actionPending = false;
    }

    @Override
    public void c() {
        super.c();
    }

    public TextActionButton getChatButton() {
        return this.chatButton;
    }

    private void handleNewPartyInviteResponse(OnlineFriend friend, GroupInviteResponsePacket response) {
        this.showInviteResult(friend, response);
    }

    private void configureRemoveButton() {
        this.removeButton.addClickListener(this::openFriendRemovalConfirmation);
    }

    @Override
    public double x() {
        return 99.0;
    }

    private void handlePartyCreationResponse(OnlineFriend friend, GroupCreateResponsePacket response) {
        if (response.getStatus() == GroupCreateStatus.SUCCESS) {
            this.actionPending = true;
            ZeusConnectionManager.T().u().J(friend.getUser(), inviteResponse -> this.handleNewPartyInviteResponse(friend, inviteResponse), this::handleNewPartyInviteFailure);
        }
    }

    private void handleFriendRemovalResponse(FriendDeleteResponsePacket response) {
        if (response.isDeleted()) {
            SonyVegas.INSTANCE.getFriendManager().removeFriend(this.friend.getExternalFriend());
            SonyVegas.INSTANCE.getOnlineFriendManager().removeFriend(this.friend);
        }
    }

    private void openFriendRemovalConfirmation() {
        if (this.actionPending) {
            return;
        }
        this.actionPending = true;
        ConfirmationDialogComponent confirmationDialog = new ConfirmationDialogComponent("Are you sure you want to remove this friend?", "REMOVE", "newtrash");
        DimmedCenteredPopupFrame confirmationPopup = ClientSettings.createPopup(this.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa(), confirmationDialog, DimmedCenteredPopupFrame.class);
        confirmationDialog.getConfirmButton().addClickListener(() -> this.confirmFriendRemoval(confirmationPopup));
        confirmationDialog.getCloseButton().addClickListener(() -> this.cancelFriendRemoval(confirmationPopup));
        confirmationPopup.addMouseListener(new OnlineFriendActionPanelPopupOutsideClickFilter(this, confirmationPopup));
    }
}
