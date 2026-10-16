package vegas.sony.friend.ui;

import vegas.sony.friend.ui.AddFriendInputPanel;
import vegas.sony.ui.click.component.input.CompactTextInputComponent;

class AddFriendInputComponent
extends CompactTextInputComponent {
    private final AddFriendInputPanel panel;

    @Override
    public void submit() {
        super.submit();
        AddFriendInputPanel.submitInput(this.panel);
    }

    AddFriendInputComponent(AddFriendInputPanel panel, String placeholder) {
        super(placeholder);
        this.panel = panel;
    }
}
