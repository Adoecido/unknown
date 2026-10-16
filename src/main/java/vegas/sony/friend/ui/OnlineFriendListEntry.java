package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.OnlineStatus;
import vegas.sony.friend.PartyInvite;
import vegas.sony.friend.PartyState;
import vegas.sony.friend.ui.OnlineFriendCard;
import vegas.sony.friend.ui.OnlineFriendListEntryClosePopupClickHandler;
import vegas.sony.friend.ui.OnlineFriendListEntryOpenActionsPopupClickHandler;
import vegas.sony.friend.ui.OnlineFriendListEntryOpenPopupClickHandler;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.GroupCreateResponsePacket;
import vegas.sony.protocol.packet.GroupCreateStatus;
import vegas.sony.protocol.packet.GroupInviteResponsePacket;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.PopupFrame;
import vegas.sony.ui.notification.NotificationMessage;
import vegas.sony.ui.notification.NotificationType;

public class OnlineFriendListEntry
extends PanelComponent {
    private boolean inviteRequestPending;
    private PopupFrame cardPopup;
    private final OnlineFriend friend;
    private static int obfuscationSeed;
    private final OnlineFriendCard friendCard;
    private boolean popupOpen;

    public static int getReservedZero() {
        int reserved = OnlineFriendListEntry.getObfuscationSeed();
        return 0;
    }

    private void detachCardToPopup() {
        this.removeChild(this.friendCard);
        this.cardPopup = ClientSettings.createPopup(this, this.friendCard, PopupFrame.class);
        this.friendCard.setPopupMode(true);
        this.l$src$V$1mibm4x();
    }

    public static void closePopup(OnlineFriendListEntry entry) {
        entry.closePopup();
    }

    private void handleExistingPartyInviteResponse(OnlineFriend friend, GroupInviteResponsePacket response) {
        this.showInviteResult(friend, response);
    }

    private void handlePartyCreationFailure() {
        this.inviteRequestPending = false;
    }

    private void handleNewPartyInviteFailure() {
        this.inviteRequestPending = false;
    }

    @Override
    public void z(boolean bl) {
        super.z(bl);
    }

    private void addLocalPartyInvite() {
        SonyVegas.INSTANCE.getOnlineManager().getPartyManager().addInvite(new PartyInvite(SonyVegas.INSTANCE.getOnlineManager().getLocalFriend()));
    }


    private void handleExistingPartyInviteFailure() {
        this.inviteRequestPending = false;
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

    private void updatePopupLayout() {
        if (!this.popupOpen) {
            return;
        }
        Frame frame = this.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa();
        this.cardPopup.K(this.G$src$D$1b2f02a());
        this.cardPopup.S(frame.n() + frame.j$src$Lvegas_sony_ui_click_frame_FrameHeaderComponent_$175vsfc().L());
        double messageListHeight = frame.L() - frame.j$src$Lvegas_sony_ui_click_frame_FrameHeaderComponent_$175vsfc().L() - 50.0;
        this.friendCard.getDetailsPanel().getChatPanel().getMessageListPanel().setExplicitHeight(messageListHeight);
        this.friendCard.getDetailsPanel().getChatPanel().getMessageListPanel().t(messageListHeight);
        this.friendCard.getDetailsPanel().getChatPanel().getMessageListPanel().l$src$V$1mibm4x();
        this.cardPopup.l$src$V$1mibm4x();
    }

    public static int getObfuscationSeed() {
        return obfuscationSeed;
    }

    private void handlePartyCreationResponse(OnlineFriend friend, GroupCreateResponsePacket response) {
        if (response.getStatus() == GroupCreateStatus.SUCCESS) {
            this.inviteRequestPending = true;
            ZeusConnectionManager.T().u().J(friend.getUser(), inviteResponse -> this.handleNewPartyInviteResponse(friend, inviteResponse), this::handleNewPartyInviteFailure);
        }
    }

    public static void openPopup(OnlineFriendListEntry entry) {
        entry.openPopup();
    }

    static {
        OnlineFriendListEntry.setObfuscationSeed(70);
    }

    private void handleNewPartyInviteResponse(OnlineFriend friend, GroupInviteResponsePacket response) {
        this.showInviteResult(friend, response);
    }

    private void closePopup() {
        ClientSettings.removePopup(this.cardPopup);
        this.addChildren(this.friendCard);
        this.friendCard.setPopupMode(false);
        this.popupOpen = false;
        this.cardPopup.l$src$V$1mibm4x();
        this.l$src$V$1mibm4x();
    }

    public OnlineFriendListEntry(OnlineFriend friend) {
        super(99.0, 24.0);
        this.friend = friend;
        this.friendCard = new OnlineFriendCard(friend);
        this.setShowDisabledOverlay(false);
        this.addChildren(this.friendCard);
        this.friendCard.getDetailsPanel().getActionPanel().getChatButton().addClickListener(new OnlineFriendListEntryOpenActionsPopupClickHandler(this));
        this.friendCard.getChatButton().addClickListener(new OnlineFriendListEntryOpenPopupClickHandler(this));
        this.friendCard.getInviteButton().addClickListener(() -> this.sendPartyInvite(friend));
        this.friendCard.getCloseButton().addClickListener(new OnlineFriendListEntryClosePopupClickHandler(this));
    }

    public OnlineFriend getFriend() {
        return this.friendCard.getFriend();
    }

    private void sendPartyInvite(OnlineFriend friend) {
        if (this.inviteRequestPending) {
            return;
        }
        if (friend.getStatus() == OnlineStatus.OFFLINE) {
            return;
        }
        this.inviteRequestPending = true;
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState != null) {
            if (!partyState.canInvite()) {
                this.inviteRequestPending = false;
                return;
            }
            if (partyState.getMembers().contains(friend)) {
                this.inviteRequestPending = false;
                OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.ERROR, "Cannot send party invite to a party member"));
                return;
            }
            if (partyState.getInvitedUsers().contains(friend)) {
                this.inviteRequestPending = false;
                OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.ERROR, "Cannot send party invite to an already invited person"));
                return;
            }
            ZeusConnectionManager.T().u().J(friend.getUser(), response -> this.handleExistingPartyInviteResponse(friend, response), this::handleExistingPartyInviteFailure);
            return;
        }
        ZeusConnectionManager.T().u().w(response -> this.handlePartyCreationResponse(friend, response), this::handlePartyCreationFailure);
    }

    @Override
    public void c() {
        super.c();
        this.updatePopupLayout();
    }

    private void openPopup() {
        this.detachCardToPopup();
        this.popupOpen = true;
    }

    public static void setObfuscationSeed(int seed) {
        obfuscationSeed = seed;
    }

    public OnlineFriendCard getFriendCard() {
        return this.friendCard;
    }

    @Override
    public double C() {
        return this.popupOpen ? super.C() : this.friendCard.L();
    }
}

