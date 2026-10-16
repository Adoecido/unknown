package vegas.sony.ui.click.component;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.IconTextActionRowComponent;
import java.awt.Point;

class IconTextActionRowForwardClickMouseListener
implements GuiMouseListener {
    final IconTextActionRowComponent row;
    final GuiClickListener clickListener;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        this.clickListener.onPrimaryClick();
    }

    IconTextActionRowForwardClickMouseListener(IconTextActionRowComponent row, GuiClickListener clickListener) {
        this.row = row;
        this.clickListener = clickListener;
    }
}
