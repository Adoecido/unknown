package vegas.sony.friend.ui;

import vegas.sony.friend.ui.CurrentPartyPanel;
import vegas.sony.ui.click.component.GuiClickListener;

public class CurrentPartyLeaveDeleteClickHandler
implements GuiClickListener {
    private final CurrentPartyPanel panel;

    public CurrentPartyLeaveDeleteClickHandler(CurrentPartyPanel panel) {
        this.panel = panel;
    }

    @Override
    public void onPrimaryClick() {
        CurrentPartyPanel.performLeaveOrDisband(this.panel);
    }
}
