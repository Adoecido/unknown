package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.impl.main.ClickGuiModulesSidecarPanel;
import java.awt.Point;

class ClickGuiModulesSidecarPrimaryMouseListener
implements GuiMouseListener {
    private final Runnable action;

    @Override
    public void g(Point point, MouseClickButton button) {
        this.action.run();
    }

    ClickGuiModulesSidecarPrimaryMouseListener(Runnable action) {
        this.action = action;
    }
}
