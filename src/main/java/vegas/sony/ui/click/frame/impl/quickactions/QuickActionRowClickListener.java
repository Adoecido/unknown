package vegas.sony.ui.click.frame.impl.quickactions;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.impl.quickactions.QuickActionRowComponent;
import java.awt.Point;

class QuickActionRowClickListener
implements GuiMouseListener {
    private final Class targetFrameClass;
    private final QuickActionRowComponent actionRow;

    QuickActionRowClickListener(QuickActionRowComponent actionRow, Class targetFrameClass) {
        this.actionRow = actionRow;
        this.targetFrameClass = targetFrameClass;
    }

    @Override
    public void g(Point clickPoint, MouseClickButton clickButton) {
        Object previousFrame = ClientSettings.getFrame(this.targetFrameClass);
        ClientSettings.showFrame(this.targetFrameClass);
        ((Frame)ClientSettings.getFrame(this.targetFrameClass)).c(((GuiComponent)previousFrame).V$src$Z$1xhop3l());
    }
}
