package vegas.sony.friend.ui;

import vegas.sony.friend.ui.IncomingFriendRequestRow;
import vegas.sony.ui.click.component.GuiClickListener;

public class IncomingFriendRequestDeclineClickHandler
implements GuiClickListener {
    final IncomingFriendRequestRow row;

    public IncomingFriendRequestDeclineClickHandler(IncomingFriendRequestRow row) {
        this.row = row;
    }

    @Override
    public void onPrimaryClick() {
        IncomingFriendRequestRow.decline(this.row);
    }
}
