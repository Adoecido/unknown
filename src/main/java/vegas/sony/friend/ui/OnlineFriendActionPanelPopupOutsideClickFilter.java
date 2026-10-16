package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendActionPanel;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.frame.PopupFrame;
import java.awt.Point;

public class OnlineFriendActionPanelPopupOutsideClickFilter
implements GuiMouseListener {
    private final OnlineFriendActionPanel actionPanel;
    private final PopupFrame popupFrame;


    @Override
    public boolean Q(Point point) {
        if (this.actionPanel.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa().getBounds().R(point) && !this.popupFrame.getBounds().R(point)) {
            return true;
        }
        return GuiMouseListener.super.Q(point);
    }

    public OnlineFriendActionPanelPopupOutsideClickFilter(OnlineFriendActionPanel onlineFriendActionPanel, PopupFrame popupFrame) {
        this.actionPanel = onlineFriendActionPanel;
        this.popupFrame = popupFrame;
    }
}

