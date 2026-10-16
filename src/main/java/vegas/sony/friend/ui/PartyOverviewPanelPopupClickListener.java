package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyDetailsPanel;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

public class PartyOverviewPanelPopupClickListener
implements GuiMouseListener {
    private final PartyDetailsPanel detailsPanel;

    public PartyOverviewPanelPopupClickListener(PartyDetailsPanel detailsPanel) {
        this.detailsPanel = detailsPanel;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        PartyDetailsPanel.openMembersPopup(this.detailsPanel);
    }
}
