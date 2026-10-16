package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyDetailsPanel;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.utils.render.GuiRenderPrimitives;

public class PartyOverviewBackgroundPanel
extends PanelComponent {
    private final PartyDetailsPanel detailsPanel;

    @Override
    public void z(boolean ignored) {
        GuiRenderPrimitives.C(this.G$src$D$1b2f02a(), this.n() - 4.0, this.A(), this.L() + 2.0 + 4.0, PartyOverviewBackgroundPanel.J.i);
    }

    public PartyOverviewBackgroundPanel(PartyDetailsPanel detailsPanel, double width, double height) {
        super(width, height);
        this.detailsPanel = detailsPanel;
    }
}
