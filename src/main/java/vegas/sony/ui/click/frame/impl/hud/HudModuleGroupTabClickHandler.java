package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.hud.HudModuleSelectorFrame;

class HudModuleGroupTabClickHandler
implements GuiClickListener {
    private final HudModuleSelectorFrame selectorFrame;
    private final HudModuleGroup group;

    @Override
    public void onPrimaryClick() {
        if (this.selectorFrame.getSelectedGroup() == this.group) {
            return;
        }
        this.selectorFrame.selectGroup(this.group);
        this.selectorFrame.getModuleListPanel().refreshModules();
        this.selectorFrame.queueForDisplay();
    }


    HudModuleGroupTabClickHandler(HudModuleSelectorFrame hudModuleSelectorFrame, HudModuleGroup hudModuleGroup) {
        this.selectorFrame = hudModuleSelectorFrame;
        this.group = hudModuleGroup;
    }
}

