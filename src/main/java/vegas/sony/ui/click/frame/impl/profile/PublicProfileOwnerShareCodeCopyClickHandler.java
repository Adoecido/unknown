package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.manager.client.PublicProfileManager;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileOwnerDetailsPanel;
import java.awt.Point;

class PublicProfileOwnerShareCodeCopyClickHandler
implements GuiMouseListener {
    private static final String SUCCESS_MESSAGE = "Copied share code to clipboard!";
    private final PublicProfileOwnerDetailsPanel ownerDetailsPanel;

    PublicProfileOwnerShareCodeCopyClickHandler(PublicProfileOwnerDetailsPanel publicProfileOwnerDetailsPanel) {
        this.ownerDetailsPanel = publicProfileOwnerDetailsPanel;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        NativeBridge.cpy(this.ownerDetailsPanel.getShareCode());
        PublicProfileManager.showInfo(SUCCESS_MESSAGE);
    }
}
