package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import vegas.sony.ui.click.component.value.EntityTargetFilterComponent;
import vegas.sony.ui.click.component.value.EntityTargetFilterDropdownCloseHandler;
import vegas.sony.ui.click.component.value.EntityTargetFilterQuickToggleComponent;
import vegas.sony.ui.click.component.value.FloatingValueDropdownLayer;
import vegas.sony.ui.click.frame.impl.profile.PublicProfilesFrameHeaderActionComponent;

public class FrameValueDropdownLayer
extends FloatingValueDropdownLayer<EntityTargetFilterComponent> {
    @Override
    public void refreshContents() {
    }

    public FrameValueDropdownLayer(EntityTargetFilterComponent entityTargetFilterComponent) {
        super(entityTargetFilterComponent);
        this.Y(new PublicProfilesFrameHeaderActionComponent(this, "newaim", "Target settings", 0.8).Q(new EntityTargetFilterDropdownCloseHandler(this, entityTargetFilterComponent)));
        this.h(new EntityTargetFilterQuickToggleComponent(entityTargetFilterComponent.getTargetFilterValue()), new Object[0]);
        this.h(new BooleanToggleComponent(entityTargetFilterComponent.getTargetFilterValue().getIgnoreInvisibleValue()), new Object[0]);
        this.h(new BooleanToggleComponent(entityTargetFilterComponent.getTargetFilterValue().getIgnoreNakedValue()), new Object[0]);
        this.h(new BooleanToggleComponent(entityTargetFilterComponent.getTargetFilterValue().getIgnoreBehindWallsValue()), new Object[0]);
    }
}
