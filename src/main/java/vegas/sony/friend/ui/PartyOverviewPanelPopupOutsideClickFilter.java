package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyDetailsPanel;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.frame.PopupFrame;
import java.awt.Point;

public class PartyOverviewPanelPopupOutsideClickFilter
implements GuiMouseListener {
    private final PopupFrame popupFrame;
    private final PartyDetailsPanel detailsPanel;


    public PartyOverviewPanelPopupOutsideClickFilter(PartyDetailsPanel partyDetailsPanel, PopupFrame popupFrame) {
        this.detailsPanel = partyDetailsPanel;
        this.popupFrame = popupFrame;
    }

    @Override
    public boolean Q(Point point) {
        if (this.detailsPanel.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa().getBounds().R(point) && !this.popupFrame.getBounds().R(point)) {
            return true;
        }
        return GuiMouseListener.super.Q(point);
    }
}

