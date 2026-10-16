package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.value.EntityTargetFilterComponent;
import vegas.sony.ui.click.frame.FrameValueDropdownLayer;

public class EntityTargetFilterDropdownCloseHandler
implements GuiClickListener {
    final FrameValueDropdownLayer dropdownLayer;
    final EntityTargetFilterComponent targetFilterComponent;

    public EntityTargetFilterDropdownCloseHandler(FrameValueDropdownLayer dropdownLayer, EntityTargetFilterComponent targetFilterComponent) {
        this.dropdownLayer = dropdownLayer;
        this.targetFilterComponent = targetFilterComponent;
    }

    @Override
    public void onPrimaryClick() {
        this.targetFilterComponent.setExpanded(false);
    }
}
