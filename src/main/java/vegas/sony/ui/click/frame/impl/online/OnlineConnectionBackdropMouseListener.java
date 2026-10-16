package vegas.sony.ui.click.frame.impl.online;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.frame.impl.online.OnlineConnectionSettingsFrame;
import java.awt.Point;

public class OnlineConnectionBackdropMouseListener
implements GuiMouseListener {
    final OnlineConnectionSettingsFrame D;


    @Override
    public boolean Q(Point point) {
        return !this.D.getBounds().R(point);
    }

    public OnlineConnectionBackdropMouseListener(OnlineConnectionSettingsFrame onlineConnectionSettingsFrame) {
        this.D = onlineConnectionSettingsFrame;
    }
}

