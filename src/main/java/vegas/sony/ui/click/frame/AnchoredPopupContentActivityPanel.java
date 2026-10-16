package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;

class AnchoredPopupContentActivityPanel
extends PanelComponent {
    final GuiComponent jM;


    @Override
    public boolean t() {
        return super.t() || this.jM.t();
    }

    AnchoredPopupContentActivityPanel(double d, double d2, GuiComponent guiComponent) {
        super(d, d2);
        this.jM = guiComponent;
    }
}
