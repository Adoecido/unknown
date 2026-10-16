package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.component.GuiClickListener;

class HudModuleConfigFrameToggleSelectedModuleClickHandler
implements GuiClickListener {
    private final HudModuleConfigFrame configFrame;

    @Override
    public void onPrimaryClick() {
        HudModule hudModule = this.configFrame.getSelectedModule();
        hudModule.setFavorite(!hudModule.isFavorite());
        if (ClientSettings.getFrame(HudModuleSelectorFrame.class).getSelectedGroup() == HudModuleGroup.FAVORITE) {
            ClientSettings.getFrame(HudModuleSelectorFrame.class).getModuleListPanel().refreshModules();
        }
    }

    HudModuleConfigFrameToggleSelectedModuleClickHandler(HudModuleConfigFrame hudModuleConfigFrame) {
        this.configFrame = hudModuleConfigFrame;
    }

}
