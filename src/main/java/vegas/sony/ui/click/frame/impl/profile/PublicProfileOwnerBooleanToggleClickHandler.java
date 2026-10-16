package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileOwnerDetailsPanel;
import java.awt.Point;

class PublicProfileOwnerBooleanToggleClickHandler
implements GuiMouseListener {
    private final BooleanToggleComponent dependentToggle;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        this.dependentToggle.setVisible(!this.dependentToggle.V$src$Z$1xhop3l());
    }

    PublicProfileOwnerBooleanToggleClickHandler(BooleanToggleComponent dependentToggle) {
        this.dependentToggle = dependentToggle;
    }

}

