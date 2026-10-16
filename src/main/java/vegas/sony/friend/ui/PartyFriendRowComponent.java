package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.FriendRequest;
import vegas.sony.friend.FriendRequestService;
import vegas.sony.friend.IncomingFriendRequest;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.OnlineFriendColorUtil;
import vegas.sony.friend.PartyState;
import vegas.sony.friend.ui.OnlineFriendAvatarComponent;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.friend.ui.PartyFriendActionLabelComponent;
import vegas.sony.friend.ui.PartyFriendCompactActionLabelComponent;
import vegas.sony.friend.ui.PartyFriendFixedTextLabelComponent;
import vegas.sony.friend.ui.PartyFriendNameLabelComponent;
import vegas.sony.friend.ui.PartyFriendSecondaryActionLabelComponent;
import vegas.sony.friend.ui.PartyFriendTertiaryActionLabelComponent;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.ClientGroupLeaderKickResponsePacket;
import vegas.sony.protocol.packet.ClientGroupLeaderKickStatus;
import vegas.sony.protocol.packet.ClientGroupLeaderPromoteResponsePacket;
import vegas.sony.protocol.packet.ClientGroupLeaderPromoteStatus;
import vegas.sony.protocol.packet.GroupUninviteResponsePacket;
import vegas.sony.protocol.packet.GroupUninviteStatus;
import vegas.sony.ui.click.component.FlowLayoutComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.TextLabelComponent;
import vegas.sony.ui.click.component.TruncatedTextComponent;
import vegas.sony.ui.click.component.gui.AnimatedCenteredTextLabelComponent;
import vegas.sony.ui.notification.NotificationMessage;
import vegas.sony.ui.notification.NotificationType;
import vegas.sony.utils.render.GuiRenderPrimitives;

public class PartyFriendRowComponent
extends PanelComponent {
    private boolean actionPending;
    protected final FlowLayoutComponent actionPanel;
    protected TextLabelComponent usernameLabel;
    private final AnimatedCenteredTextLabelComponent kickAction;
    protected final AnimatedCenteredTextLabelComponent revokeAction;
    private boolean reservedState;
    protected final OnlineFriend friend;
    private final AnimatedCenteredTextLabelComponent promoteAction;
    private final AnimatedCenteredTextLabelComponent addFriendAction;
    private final FlowLayoutComponent contentPanel = new FlowLayoutComponent(70.0);
    private final boolean limitedActions;
    private final boolean invitedUser;
    private static String obfuscationName;
    private final TruncatedTextComponent nameLabel;

    private void handlePromoteResponse(PartyState partyState, ClientGroupLeaderPromoteResponsePacket response) {
        if (response.M() == ClientGroupLeaderPromoteStatus.SUCCESS) {
            partyState.setLeader(this.friend);
        }
    }


    @Override
    public void H() {
        super.H();
    }

    public PartyFriendRowComponent(OnlineFriend friend, boolean invitedUser, boolean limitedActions) {
        super(99.0, 20.0);
        this.promoteAction = new PartyFriendCompactActionLabelComponent(this, "PROMOTE", PartyFriendRowComponent.J.l);
        this.kickAction = new PartyFriendSecondaryActionLabelComponent(this, "KICK", PartyFriendRowComponent.J.l);
        this.revokeAction = new PartyFriendActionLabelComponent(this, "REVOKE", PartyFriendRowComponent.J.l);
        this.addFriendAction = new PartyFriendTertiaryActionLabelComponent(this, "ADD", PartyFriendRowComponent.J.l);
        this.actionPanel = new FlowLayoutComponent(70.0);
        this.actionPending = false;
        this.friend = friend;
        this.invitedUser = invitedUser;
        this.limitedActions = limitedActions;
        this.nameLabel = new PartyFriendNameLabelComponent(this, friend.getDisplayName(), "...", 72.0, 0.75, PartyFriendRowComponent.J.A, false);
        this.usernameLabel = new PartyFriendFixedTextLabelComponent(this, friend.getMinecraftUsername(), 0.55, 0.75, 0.1, 72.0, false, false, PartyFriendRowComponent.J.h);
        this.contentPanel.h(this.nameLabel, new Object[0]);
        this.actionPanel.h(this.usernameLabel, new Object[0]);
        if (invitedUser) {
            this.actionPanel.addChildren(this.revokeAction);
        } else {
            this.actionPanel.addChildren(this.addFriendAction, this.promoteAction, new SpacerComponent(2.0, 1.0), this.kickAction);
        }
        this.contentPanel.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.contentPanel.addChildren(new SpacerComponent(0.0, 9.0));
        this.contentPanel.addChildren(this.actionPanel);
        this.addChildren(new SpacerComponent(4.0, 1.0), new OnlineFriendAvatarComponent(friend, 8.0, 8.0), new SpacerComponent(4.0, 1.0), this.contentPanel);
        this.promoteAction.addClickListener(this::promoteToLeader);
        this.kickAction.addClickListener(this::kickFromParty);
        this.revokeAction.addClickListener(this::revokeInvite);
        this.addFriendAction.addClickListener(this::addAsFriend);
    }

    public static String getName() {
        return obfuscationName;
    }

    private void handleRevokeResponse(PartyState partyState, GroupUninviteResponsePacket response) {
        if (response.getStatus() == GroupUninviteStatus.SUCCESS) {
            partyState.removeMember(this.friend);
        }
    }

    protected void renderRoleIndicator() {
        if (!this.invitedUser) {
            GuiRenderPrimitives.V(this.usernameLabel.G$src$D$1b2f02a(), this.usernameLabel.n() + 2.0, 2.0, 1.0, OnlineFriendColorUtil.getGroupRoleColor(this.friend.getGroupRole()));
        }
    }

    static {
        PartyFriendRowComponent.setObfuscationName("yJCYxb");
    }

    private void promoteToLeader() {
        if (this.actionPending) {
            return;
        }
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState == null) {
            return;
        }
        this.actionPending = true;
        ZeusConnectionManager.T().u().s(this.friend.getUser(), response -> this.handlePromoteResponse(partyState, response), this::handlePromoteFailure);
    }

    @Override
    public void c() {
        boolean alreadyFriendOrRequested = SonyVegas.INSTANCE.getOnlineFriendManager().getFriends().contains(this.friend) || SonyVegas.INSTANCE.getOnlineManager().getFriendRequestManager().hasOutgoingRequest(this.friend);
        boolean localUser = SonyVegas.INSTANCE.getOnlineManager().getLocalFriend().equals(this.friend);
        boolean canAddFriend = !alreadyFriendOrRequested && !localUser;
        if (this.w$src$Z$e457mb()) {
            if (this.limitedActions) {
                this.addFriendAction.setVisible(canAddFriend);
                this.promoteAction.setVisible(false);
                this.kickAction.setVisible(false);
                this.revokeAction.setVisible(false);
            } else {
                this.addFriendAction.setVisible(canAddFriend);
                this.promoteAction.setVisible(true);
                this.kickAction.setVisible(true);
                this.revokeAction.setVisible(true);
            }
        } else {
            this.addFriendAction.setVisible(false);
            this.promoteAction.setVisible(false);
            this.kickAction.setVisible(false);
            this.revokeAction.setVisible(false);
        }
        this.usernameLabel.setVisible(!this.w$src$Z$e457mb() || this.limitedActions && !canAddFriend);
        this.l$src$V$1mibm4x();
        this.nameLabel.setText(this.friend.getDisplayName());
        if (this.invitedUser) {
            this.usernameLabel.setText(this.friend.getMinecraftUsername());
        } else {
            this.usernameLabel.setText("   " + this.friend.getMinecraftUsername());
        }
        super.c();
        this.renderRoleIndicator();
        this.promoteAction.setFontScale((double)0.65f);
        this.kickAction.setFontScale((double)0.65f);
        this.revokeAction.setFontScale((double)0.65f);
        this.addFriendAction.setFontScale((double)0.65f);
        this.promoteAction.setBorderAlpha(1.0f);
        this.kickAction.setBorderAlpha(1.0f);
        this.revokeAction.setBorderAlpha(1.0f);
        this.addFriendAction.setBorderAlpha(1.0f);
        this.contentPanel.setShowDisabledOverlay(false);
        this.setShowDisabledOverlay(false);
    }

    private void handlePromoteFailure() {
        this.actionPending = false;
    }

    private void handleKickFailure() {
        this.actionPending = false;
    }

    private void handleRevokeFailure() {
        this.actionPending = false;
    }

    public OnlineFriend getFriend() {
        return this.friend;
    }

    private void kickFromParty() {
        if (this.actionPending) {
            return;
        }
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState == null) {
            return;
        }
        this.actionPending = true;
        ZeusConnectionManager.T().u().c(this.friend.getUser(), response -> this.handleKickResponse(partyState, response), this::handleKickFailure);
    }

    private void handleKickResponse(PartyState partyState, ClientGroupLeaderKickResponsePacket response) {
        if (response.P() == ClientGroupLeaderKickStatus.SUCCESS) {
            partyState.removeMember(this.friend);
        }
    }

    private void addAsFriend() {
        if (this.actionPending) {
            return;
        }
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState == null) {
            return;
        }
        for (FriendRequest friendRequest : SonyVegas.INSTANCE.getOnlineManager().getFriendRequestManager().getIncomingRequests()) {
            if (!friendRequest.getFriend().getDisplayName().equals(this.friend.getDisplayName())) continue;
            OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.SUCCESS, "Added " + this.friend.getDisplayName() + " as a friend"));
            SonyVegas.INSTANCE.getOnlineManager().getFriendRequestManager().acceptIncomingRequest((IncomingFriendRequest)friendRequest);
            return;
        }
        this.actionPending = true;
        FriendRequestService.sendFriendRequest(this.friend.getDisplayName());
        this.actionPending = false;
    }

    public static void setObfuscationName(String name) {
        obfuscationName = name;
    }

    private void revokeInvite() {
        if (this.actionPending) {
            return;
        }
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState == null) {
            return;
        }
        this.actionPending = true;
        ZeusConnectionManager.T().u().V(this.friend.getUser(), response -> this.handleRevokeResponse(partyState, response), this::handleRevokeFailure);
    }
}

