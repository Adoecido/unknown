package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileOverlayPopupFrame;
import java.awt.Point;

class PublicProfileOverlayCloseClickHandler
implements GuiMouseListener {
    final PublicProfileOverlayPopupFrame popupFrame;

    PublicProfileOverlayCloseClickHandler(PublicProfileOverlayPopupFrame publicProfileOverlayPopupFrame) {
        this.popupFrame = publicProfileOverlayPopupFrame;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        ClientSettings.removePopup(this.popupFrame);
    }
}
