package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.ui.click.component.GuiKeyTypedListener;
import vegas.sony.ui.click.frame.impl.hud.HudModuleSearchBox;
import vegas.sony.ui.click.frame.impl.hud.HudModuleSelectorFrame;

public class HudModuleSearchKeyHandler
implements GuiKeyTypedListener {
    private final HudModuleSelectorFrame selectorFrame;
    private final HudModuleSearchBox searchBox;

    @Override
    public void onKeyTyped(char c, int n) {
        this.selectorFrame.setSearchQuery(this.searchBox.getInput().getText());
    }

    public HudModuleSearchKeyHandler(HudModuleSearchBox hudModuleSearchBox, HudModuleSelectorFrame hudModuleSelectorFrame) {
        this.searchBox = hudModuleSearchBox;
        this.selectorFrame = hudModuleSelectorFrame;
    }
}
