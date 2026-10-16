package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyDetailsPanel;
import vegas.sony.ui.click.GuiMouseListener;
import java.awt.Point;

public class PartyOverviewPanelPopupOutsideClickListener
implements GuiMouseListener {
    private final PartyDetailsPanel detailsPanel;

    public PartyOverviewPanelPopupOutsideClickListener(PartyDetailsPanel partyDetailsPanel) {
        this.detailsPanel = partyDetailsPanel;
    }

    @Override
    public boolean Q(Point point) {
        if (!PartyDetailsPanel.getSettingsPopup(this.detailsPanel).getBounds().R(point) && !PartyDetailsPanel.getSettingsPanel(this.detailsPanel).getBounds().R(point)) {
            PartyDetailsPanel.closeSettingsPopup(this.detailsPanel);
            return true;
        }
        return GuiMouseListener.super.Q(point);
    }

}

