package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyDetailsPanel;
import vegas.sony.ui.click.component.GuiClickListener;

public class PartyOverviewPanelActionClickHandler
implements GuiClickListener {
    private final PartyDetailsPanel detailsPanel;

    @Override
    public void onPrimaryClick() {
        PartyDetailsPanel.toggleSettingsPopup(this.detailsPanel);
    }

    public PartyOverviewPanelActionClickHandler(PartyDetailsPanel detailsPanel) {
        this.detailsPanel = detailsPanel;
    }
}
