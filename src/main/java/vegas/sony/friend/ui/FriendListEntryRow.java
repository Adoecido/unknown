package vegas.sony.friend.ui;

import vegas.sony.friend.ExternalFriend;
import vegas.sony.friend.FriendEntry;
import vegas.sony.friend.ui.FriendListEntryRemoveClickHandler;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.MouseButton;
import vegas.sony.ui.click.component.SelectableTextRowComponent;

public class FriendListEntryRow
extends SelectableTextRowComponent {
    private final FriendEntry friendEntry;

    @Override
    public String getText() {
        return super.getText();
    }

    @Override
    public void H() {
        super.H();
        String text = this.friendEntry.getName();
        if (!this.isHovered()) {
            if (!this.friendEntry.getDisplayName().equals(this.friendEntry.getName())) {
                text = "*" + this.friendEntry.getAlias();
            }
        } else if (this.friendEntry instanceof ExternalFriend) {
            ExternalFriend externalFriend = (ExternalFriend)this.friendEntry;
            text = "*" + externalFriend.getOnlineFriend().getDisplayName();
        }
        this.setText(text);
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
        super.g(guiMouseEvent);
        if (guiMouseEvent.getAction().equals((Object)MouseButton.LEFT_CLICK)) {
            this.friendEntry.setTargeted(!this.friendEntry.isTargeted());
        }
    }

    public FriendListEntryRow(FriendEntry friendEntry) {
        super(FriendListEntryRow.J.B, friendEntry.getName());
        this.friendEntry = friendEntry;
        if (friendEntry instanceof ExternalFriend) {
            this.setIndicatorIcon("synced@2x");
            this.setSelectedIndicatorColor(FriendListEntryRow.J.T);
        }
        this.setHorizontalInset(0.0f);
        this.o(99.0);
        this.setUseExplicitWidth(true);
        this.setDeleteActionListener(new FriendListEntryRemoveClickHandler(this, friendEntry));
        this.w("Toggle friend between Active and Inactive");
        this.getDeleteButton().w("Remove friend from list");
    }


    @Override
    public boolean isSelected() {
        return this.friendEntry.isTargeted();
    }
}

