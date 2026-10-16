package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.PartyInvite;
import vegas.sony.friend.PartyState;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.GroupDeleteResponsePacket;
import vegas.sony.protocol.packet.GroupInviteStateResponsePacket;
import vegas.sony.protocol.packet.GroupInviteStateStatus;
import vegas.sony.ui.click.component.ConfirmationDialogComponent;
import vegas.sony.ui.click.component.IconButtonComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.SquareIconButtonComponent;
import vegas.sony.ui.click.component.TruncatedTextComponent;
import vegas.sony.ui.click.component.gui.TextButton;
import vegas.sony.ui.click.frame.DimmedCenteredPopupFrame;
import vegas.sony.ui.click.frame.PopupFrame;
import vegas.sony.ui.notification.NotificationType;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.ImageRenderer;
import java.awt.Color;

public class PartyInviteRow
extends PanelComponent {
    private final SpacerComponent iconSpacer;
    private final SpacerComponent declineButtonSpacer;
    private final PanelComponent declinePanel;
    private final PartyInvite invite;
    private final TruncatedTextComponent inviterNameLabel;
    private final TextButton joinButton;
    private final PanelComponent joinPanel = new PanelComponent(18.0, 14.0);
    private boolean actionPending;
    private final IconButtonComponent declineButton;


    private void handleAcceptFailure() {
        this.actionPending = false;
    }

    private void handleLeaveSuccess() {
        this.acceptInvite();
    }

    private void attemptAcceptInvite() {
        if (this.actionPending) {
            return;
        }
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState != null) {
            if (partyState.getLeader().equals(SonyVegas.INSTANCE.getOnlineManager().getLocalFriend())) {
                this.showCurrentPartyLeaveConfirmation("Are you sure you want to disband the party?");
            } else {
                this.showCurrentPartyLeaveConfirmation("Are you sure you want to leave your current party?");
            }
            return;
        }
        this.actionPending = true;
        this.acceptInvite();
    }

    public PartyInviteRow(PartyInvite partyInvite) {
        super(100.0, 16.0);
        this.declinePanel = new PanelComponent(14.0, 14.0);
        this.iconSpacer = new SpacerComponent(18.0, 16.0);
        this.declineButtonSpacer = new SpacerComponent(2.0, 16.0);
        this.joinButton = new TextButton("JOIN", 0.6, PartyInviteRow.J.B, PartyInviteRow.J.O, 18.0, 8.0);
        this.declineButton = new SquareIconButtonComponent("newclose", 1.0, new Color(255, 255, 255, 0), new Color(255, 255, 255, 25), 8.0, 8.0);
        this.actionPending = false;
        this.invite = partyInvite;
        this.inviterNameLabel = new TruncatedTextComponent(partyInvite.getInviter().getDisplayName(), "...", 46.0, 0.8, PartyInviteRow.J.A, true);
        this.setShowDisabledOverlay(false);
        this.joinPanel.setShowDisabledOverlay(false);
        this.declinePanel.setShowDisabledOverlay(false);
        this.joinPanel.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().Q(true);
        this.declinePanel.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().Q(true);
        this.declineButton.w("Decline Invite");
        this.joinButton.addClickListener(this::attemptAcceptInvite);
        this.declineButton.addClickListener(() -> this.declineInvite(partyInvite));
        this.joinButton.setDeriveTextColorFromBackground(false);
        this.joinButton.setNormalTextColor(Color.WHITE);
        this.joinPanel.addChildren(this.joinButton);
        this.declinePanel.addChildren(this.declineButtonSpacer, this.declineButton);
        this.addChildren(this.iconSpacer, this.inviterNameLabel, this.joinPanel, this.declinePanel);
    }

    public PartyInvite getInvite() {
        return this.invite;
    }

    private static void handleCurrentPartyLeaveResponse(GroupDeleteResponsePacket response) {
    }

    private void declineInvite(PartyInvite invite) {
        if (this.actionPending) {
            return;
        }
        this.actionPending = true;
        ZeusConnectionManager.T().u().c(invite.getInviter().getUser(), false, response -> PartyInviteRow.handleDeclineResponse(invite, response), this::handleDeclineFailure);
    }

    @Override
    public void H() {
        this.joinButton.setDeriveTextColorFromBackground(false);
        GuiRenderPrimitives.d(this.G$src$D$1b2f02a(), this.n(), this.A(), this.L() - 2.0, PartyInviteRow.J.m.brighter());
        float iconX = (float)(this.G$src$D$1b2f02a() + 6.0);
        float iconY = (float)(this.n() + 4.0);
        ImageRenderer.drawImage(PartyInviteRow.J.B, iconX, iconY, "party1@2x", 7.0f, 6.3f, false);
        ImageRenderer.drawImage(PartyInviteRow.J.B, (float)(this.G$src$D$1b2f02a() + this.A() - 22.0), (float)this.n() - 0.5f, "join party texture@2x", 14.5f, 14.5f, false);
        this.inviterNameLabel.S(this.n() + 5.0);
        this.inviterNameLabel.setExplicitWidth(this.A() - 18.0 - this.declineButton.A() - this.joinButton.A() - 4.0);
        this.inviterNameLabel.setMaxWidth(this.A() - 18.0 - this.declineButton.A() - this.joinButton.A() - 6.0);
    }

    private void acceptInvite() {
        ZeusConnectionManager.T().u().c(this.invite.getInviter().getUser(), true, this::handleAcceptResponse, this::handleAcceptFailure);
    }

    private void cancelCurrentPartyLeave(PopupFrame popupFrame) {
        ClientSettings.removePopup(popupFrame);
        this.actionPending = false;
    }

    private void confirmCurrentPartyLeave(PopupFrame popupFrame) {
        ClientSettings.removePopup(popupFrame);
        ZeusConnectionManager.T().u().l(PartyInviteRow::handleCurrentPartyLeaveResponse, this::handleLeaveSuccess);
    }

    private static void handleDeclineResponse(PartyInvite invite, GroupInviteStateResponsePacket response) {
        if (response.getStatus() == GroupInviteStateStatus.SUCCESSFULLY_DECLINED) {
            SonyVegas.INSTANCE.getOnlineManager().getPartyManager().removeInvite(invite);
        } else if (response.getStatus() == GroupInviteStateStatus.FAILED) {
            OnlineFriendUiHelper.showNotification(NotificationType.ERROR, "Error declining party invite");
        }
    }

    private void showCurrentPartyLeaveConfirmation(String message) {
        ConfirmationDialogComponent confirmationDialog = new ConfirmationDialogComponent(message, "DISBAND", "disband confirm@2x");
        DimmedCenteredPopupFrame confirmationPopup = ClientSettings.createPopup(this.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa(), confirmationDialog, DimmedCenteredPopupFrame.class);
        confirmationDialog.getConfirmButton().addClickListener(() -> this.confirmCurrentPartyLeave(confirmationPopup));
        confirmationDialog.getCloseButton().addClickListener(() -> this.cancelCurrentPartyLeave(confirmationPopup));
        confirmationPopup.q(this.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa(), confirmationPopup);
    }

    private void handleAcceptResponse(GroupInviteStateResponsePacket response) {
        if (response.getStatus() == GroupInviteStateStatus.SUCCESSFULLY_ACCEPTED) {
            SonyVegas.INSTANCE.getOnlineManager().getPartyManager().removeInvite(this.invite);
        } else if (response.getStatus() == GroupInviteStateStatus.GROUP_FULL) {
            OnlineFriendUiHelper.showNotification(NotificationType.ERROR, "Party is full");
        } else if (response.getStatus() == GroupInviteStateStatus.FAILED) {
            OnlineFriendUiHelper.showNotification(NotificationType.ERROR, "Error accepting party invite");
        }
    }

    private void handleDeclineFailure() {
        this.actionPending = false;
    }
}

