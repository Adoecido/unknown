package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OutgoingFriendRequestRow;
import vegas.sony.ui.click.component.GuiClickListener;

public class OutgoingFriendRequestCancelClickHandler
implements GuiClickListener {
    final OutgoingFriendRequestRow row;

    public OutgoingFriendRequestCancelClickHandler(OutgoingFriendRequestRow row) {
        this.row = row;
    }

    @Override
    public void onPrimaryClick() {
        OutgoingFriendRequestRow.cancel(this.row);
    }
}
