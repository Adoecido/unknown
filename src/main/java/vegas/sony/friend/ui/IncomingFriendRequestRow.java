package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.IncomingFriendRequest;
import vegas.sony.friend.ui.FriendRequestRow;
import vegas.sony.friend.ui.IncomingFriendRequestAcceptClickHandler;
import vegas.sony.friend.ui.IncomingFriendRequestDeclineClickHandler;

public class IncomingFriendRequestRow
extends FriendRequestRow {
    private final IncomingFriendRequest request;

    public static void accept(IncomingFriendRequestRow row) {
        row.acceptRequest();
    }

    public IncomingFriendRequestRow(IncomingFriendRequest incomingFriendRequest) {
        super(incomingFriendRequest, null);
        this.request = incomingFriendRequest;
        this.getAddButton().addClickListener(new IncomingFriendRequestAcceptClickHandler(this));
        this.getActionPanel().h(this.getAddButton(), new Object[0]);
        this.getCloseButton().addClickListener(new IncomingFriendRequestDeclineClickHandler(this));
    }

    private void declineRequest() {
        SonyVegas.INSTANCE.getOnlineManager().getFriendRequestManager().declineIncomingRequest(this.request);
    }

    private void acceptRequest() {
        SonyVegas.INSTANCE.getOnlineManager().getFriendRequestManager().acceptIncomingRequest(this.request);
    }

    public static void decline(IncomingFriendRequestRow row) {
        row.declineRequest();
    }
}
