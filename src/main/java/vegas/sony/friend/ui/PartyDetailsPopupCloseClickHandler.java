package vegas.sony.friend.ui;

import vegas.sony.friend.ui.CurrentPartyPanel;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;

public class PartyDetailsPopupCloseClickHandler
implements GuiClickListener {
    private final CurrentPartyPanel panel;

    @Override
    public void onPrimaryClick() {
        ClientSettings.removePopup(CurrentPartyPanel.getDetailsPopup(this.panel));
        CurrentPartyPanel.setDetailsPanel(this.panel, null);
        CurrentPartyPanel.setDetailsPopup(this.panel, null);
    }

    public PartyDetailsPopupCloseClickHandler(CurrentPartyPanel panel) {
        this.panel = panel;
    }
}
