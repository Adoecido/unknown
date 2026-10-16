package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrameHeader;

public class ModuleCategoryFrameHeaderClearModuleSelectionClickHandler
implements GuiClickListener {
    final ModuleCategoryFrameHeader q;

    public ModuleCategoryFrameHeaderClearModuleSelectionClickHandler(ModuleCategoryFrameHeader yy_22) {
        this.q = yy_22;
    }

    @Override
    public void onPrimaryClick() {
        this.q.h();
    }
}
