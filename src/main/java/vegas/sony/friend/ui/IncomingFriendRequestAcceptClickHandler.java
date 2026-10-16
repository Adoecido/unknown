package vegas.sony.friend.ui;

import vegas.sony.friend.ui.IncomingFriendRequestRow;
import vegas.sony.ui.click.component.GuiClickListener;

public class IncomingFriendRequestAcceptClickHandler
implements GuiClickListener {
    final IncomingFriendRequestRow row;

    @Override
    public void onPrimaryClick() {
        IncomingFriendRequestRow.accept(this.row);
    }

    public IncomingFriendRequestAcceptClickHandler(IncomingFriendRequestRow row) {
        this.row = row;
    }
}
