package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendListEntry;
import vegas.sony.ui.click.component.GuiClickListener;

public class OnlineFriendListEntryClosePopupClickHandler
implements GuiClickListener {
    private final OnlineFriendListEntry entry;

    @Override
    public void onPrimaryClick() {
        OnlineFriendListEntry.closePopup(this.entry);
    }

    public OnlineFriendListEntryClosePopupClickHandler(OnlineFriendListEntry entry) {
        this.entry = entry;
    }
}
