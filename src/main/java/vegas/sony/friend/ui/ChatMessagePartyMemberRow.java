package vegas.sony.friend.ui;

import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.ui.PartyMemberRow;
import vegas.sony.friend.ui.PartyMemberStatusComponent;

public class ChatMessagePartyMemberRow
extends PartyMemberRow {
    public ChatMessagePartyMemberRow(OnlineFriend onlineFriend, PartyMemberStatusComponent partyMemberStatusComponent) {
        super(onlineFriend, partyMemberStatusComponent, false);
        this.getAvatar().setVisible(false);
    }
}
