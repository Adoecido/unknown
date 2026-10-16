package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import java.awt.Point;

class BooleanToggleComponentClickMouseListener
implements GuiMouseListener {
    private final BooleanToggleComponent toggleComponent;

    BooleanToggleComponentClickMouseListener(BooleanToggleComponent toggleComponent) {
        this.toggleComponent = toggleComponent;
    }

    @Override
    public void g(Point clickPoint, MouseClickButton clickButton) {
        this.toggleComponent.toggleIfInteractive();
    }
}
