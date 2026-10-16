package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineActivitySettingsFrame;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

public class OnlineActivityPanelRefreshClickHandler
implements GuiMouseListener {
    private final OnlineActivitySettingsFrame settingsFrame;

    public OnlineActivityPanelRefreshClickHandler(OnlineActivitySettingsFrame onlineActivitySettingsFrame) {
        this.settingsFrame = onlineActivitySettingsFrame;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (mouseClickButton != MouseClickButton.LEFT_CLICK) {
            return;
        }
        OnlineActivitySettingsFrame.getActivityListPanel(this.settingsFrame).requestRefresh();
    }

}

