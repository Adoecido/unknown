package vegas.sony.friend.ui;

import vegas.sony.friend.FriendEntry;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.SelectableTextRowComponent;

public class FriendSettingsEntryRowComponent
extends SelectableTextRowComponent {
    private final FriendEntry friendEntry;

    @Override
    public boolean isSelected() {
        return this.friendEntry.isTargeted();
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
        this.friendEntry.setTargeted(!this.friendEntry.isTargeted());
    }

    @Override
    public void H() {
        String text = this.friendEntry.getName();
        if (!this.friendEntry.getDisplayName().equals(this.friendEntry.getName()) && !this.isHovered()) {
            text = "*" + this.friendEntry.getAlias();
        }
        this.setText(text);
        super.H();
    }


    public FriendSettingsEntryRowComponent(FriendEntry friendEntry) {
        super(FriendSettingsEntryRowComponent.J.B, friendEntry.getName());
        this.friendEntry = friendEntry;
    }

    public FriendEntry getFriendEntry() {
        return this.friendEntry;
    }
}

