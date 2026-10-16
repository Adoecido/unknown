package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendListEntry;
import vegas.sony.ui.click.component.GuiClickListener;

public class OnlineFriendListEntryOpenActionsPopupClickHandler
implements GuiClickListener {
    private final OnlineFriendListEntry entry;

    public OnlineFriendListEntryOpenActionsPopupClickHandler(OnlineFriendListEntry onlineFriendListEntry) {
        this.entry = onlineFriendListEntry;
    }

    @Override
    public void onPrimaryClick() {
        OnlineFriendListEntry.openPopup(this.entry);
    }
}
