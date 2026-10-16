package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.SimpleTextLabelComponent;
import vegas.sony.ui.click.frame.impl.hud.HudModuleConfigFrame;
import vegas.sony.ui.click.frame.impl.hud.HudModuleSelectorHeaderComponent;

class HudModuleSelectorOpenConfigFrameClickHandler
implements GuiClickListener {
    private final HudModuleSelectorHeaderComponent header;

    HudModuleSelectorOpenConfigFrameClickHandler(HudModuleSelectorHeaderComponent hudModuleSelectorHeaderComponent) {
        this.header = hudModuleSelectorHeaderComponent;
    }

    @Override
    public void onPrimaryClick() {
        HudModuleConfigFrame hudModuleConfigFrame = ClientSettings.getFrame(HudModuleConfigFrame.class);
        if (hudModuleConfigFrame == null) {
            return;
        }
        hudModuleConfigFrame.setTitle("Settings");
        hudModuleConfigFrame.removeMarkedChildren();
        hudModuleConfigFrame.h(new SimpleTextLabelComponent("No settings yet"), new Object[0]);
        this.header.getSettingsButton().setVisible(true);
        hudModuleConfigFrame.setVisible(true);
        hudModuleConfigFrame.U();
        hudModuleConfigFrame.t(hudModuleConfigFrame.L());
        hudModuleConfigFrame.beginOpening();
        hudModuleConfigFrame.l$src$V$1mibm4x();
    }

}

