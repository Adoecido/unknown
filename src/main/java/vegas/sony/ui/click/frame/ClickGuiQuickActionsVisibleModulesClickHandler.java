package vegas.sony.ui.click.frame;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.ClickGuiQuickActionsComponent;
import vegas.sony.ui.click.frame.impl.VisibleModuleListFrame;

public class ClickGuiQuickActionsVisibleModulesClickHandler
implements GuiClickListener {
    final ClickGuiQuickActionsComponent v;

    @Override
    public void onPrimaryClick() {
        ClientSettings.showFrame(VisibleModuleListFrame.class);
    }

    public ClickGuiQuickActionsVisibleModulesClickHandler(ClickGuiQuickActionsComponent clickGuiQuickActionsComponent) {
        this.v = clickGuiQuickActionsComponent;
    }
}
