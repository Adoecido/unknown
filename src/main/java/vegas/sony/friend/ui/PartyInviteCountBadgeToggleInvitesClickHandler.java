package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.friend.ui.PartyInviteCountBadge;
import vegas.sony.ui.click.component.GuiClickListener;

class PartyInviteCountBadgeToggleInvitesClickHandler
implements GuiClickListener {
    private final PartyInviteCountBadge badge;

    @Override
    public void onPrimaryClick() {
        OnlineFriendUiHelper.findOnlineFriendsFrame(this.badge).getPartyInvitesPanel().toggleExpanded();
    }

    PartyInviteCountBadgeToggleInvitesClickHandler(PartyInviteCountBadge partyInviteCountBadge) {
        this.badge = partyInviteCountBadge;
    }
}
