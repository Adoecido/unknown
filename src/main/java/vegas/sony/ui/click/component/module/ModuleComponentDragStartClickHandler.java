package vegas.sony.ui.click.component.module;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.MousePosition;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.utils.render.RenderUtils;

class ModuleComponentDragStartClickHandler
implements GuiClickListener {
    final ModuleComponent owner;

    ModuleComponentDragStartClickHandler(ModuleComponent owner) {
        this.owner = owner;
    }

    @Override
    public void onPrimaryClick() {
        MousePosition mousePosition = RenderUtils.h();
        this.owner.setLastDragMouseY(mousePosition.H);
        this.owner.setDragStartY(this.owner.double_n());
        this.owner.setDragging(true);
        ClientSettings.activeComponent = this.owner;
    }
}
