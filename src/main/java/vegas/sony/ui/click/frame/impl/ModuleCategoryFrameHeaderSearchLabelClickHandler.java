package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrameHeader;

public class ModuleCategoryFrameHeaderSearchLabelClickHandler
implements GuiClickListener {
    final ModuleCategoryFrameHeader r;


    @Override
    public void onPrimaryClick() {
        boolean bl;
        ClientSettings.moduleSearchActive = bl = !ClientSettings.moduleSearchActive;
        ClientSettings.refreshModuleCategoryHeaders();
    }

    public ModuleCategoryFrameHeaderSearchLabelClickHandler(ModuleCategoryFrameHeader moduleCategoryFrameHeader) {
        this.r = moduleCategoryFrameHeader;
    }
}

