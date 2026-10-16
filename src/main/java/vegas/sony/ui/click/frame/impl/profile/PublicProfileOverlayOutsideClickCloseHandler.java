package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileOverlayPopupFrame;
import java.awt.Point;

class PublicProfileOverlayOutsideClickCloseHandler
implements GuiMouseListener {
    final PublicProfileOverlayPopupFrame popupFrame;

    PublicProfileOverlayOutsideClickCloseHandler(PublicProfileOverlayPopupFrame publicProfileOverlayPopupFrame) {
        this.popupFrame = publicProfileOverlayPopupFrame;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (!PublicProfileOverlayPopupFrame.isOutsideClickCloseEnabled(this.popupFrame)) {
            return;
        }
        if (!this.popupFrame.w$src$Z$e457mb()) {
            ClientSettings.removePopup(this.popupFrame);
        }
    }

}

