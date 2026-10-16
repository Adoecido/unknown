package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.hud.HudModuleListPanel;
import vegas.sony.ui.click.frame.impl.hud.HudModuleOverviewFrame;
import vegas.sony.ui.click.frame.impl.hud.HudModuleSelectorFrame;

class HudModuleOverviewOpenSelectorClickHandler
implements GuiClickListener {
    @Override
    public void onPrimaryClick() {
        ClientSettings.getFrame(HudModuleListPanel.class).refreshModules();
        ClientSettings.showFrame(HudModuleSelectorFrame.class);
        HudModuleSelectorFrame.overviewVisible = false;
        ClientSettings.getFrame(HudModuleOverviewFrame.class).setVisible(false);
    }
}
