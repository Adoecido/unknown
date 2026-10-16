package vegas.sony.friend.ui;

import vegas.sony.friend.ui.CurrentPartyPanel;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

public class CurrentPartyPanelOpenDetailsMouseListener
implements GuiMouseListener {
    private final CurrentPartyPanel panel;

    public CurrentPartyPanelOpenDetailsMouseListener(CurrentPartyPanel currentPartyPanel) {
        this.panel = currentPartyPanel;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        CurrentPartyPanel.openDetails(this.panel, point, mouseClickButton);
    }
}
