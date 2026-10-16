package vegas.sony.ui.click.frame.impl;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrameHeader;

public class ModuleCategoryFrameHeaderSearchIconClickHandler
implements GuiClickListener {
    final ModuleCategoryFrameHeader m;

    @Override
    public void onPrimaryClick() {
        boolean bl;
        ClientSettings.moduleSearchActive = bl = !ClientSettings.moduleSearchActive;
        ClientSettings.refreshModuleCategoryHeaders();
    }


    public ModuleCategoryFrameHeaderSearchIconClickHandler(ModuleCategoryFrameHeader moduleCategoryFrameHeader) {
        this.m = moduleCategoryFrameHeader;
    }
}

