package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendListEntry;
import vegas.sony.ui.click.component.GuiClickListener;

public class OnlineFriendListEntryOpenPopupClickHandler
implements GuiClickListener {
    private final OnlineFriendListEntry entry;

    @Override
    public void onPrimaryClick() {
        OnlineFriendListEntry.openPopup(this.entry);
    }

    public OnlineFriendListEntryOpenPopupClickHandler(OnlineFriendListEntry entry) {
        this.entry = entry;
    }
}
