package vegas.sony.ui.click.frame;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.ClickGuiQuickActionsComponent;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrame;
import vegas.sony.ui.click.frame.impl.quickactions.QuickActionsFrame;

class ClickGuiQuickActionsFrameToggleClickHandler
implements GuiClickListener {
    final ClickGuiQuickActionsComponent z;


    @Override
    public void onPrimaryClick() {
        QuickActionsFrame quickActionsFrame = ClientSettings.getFrame(QuickActionsFrame.class);
        ClientSettingsSearchFrame clientSettingsSearchFrame = ClientSettings.getFrame(ClientSettingsSearchFrame.class);
        if (quickActionsFrame == null || clientSettingsSearchFrame == null) {
            return;
        }
        quickActionsFrame.setVisible(!quickActionsFrame.V$src$Z$1xhop3l());
        if (quickActionsFrame.V$src$Z$1xhop3l()) {
            quickActionsFrame.U();
            quickActionsFrame.w(1);
        }
        quickActionsFrame.l$src$V$1mibm4x();
    }

    ClickGuiQuickActionsFrameToggleClickHandler(ClickGuiQuickActionsComponent clickGuiQuickActionsComponent) {
        this.z = clickGuiQuickActionsComponent;
    }
}

