package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.SonyVegas;
import vegas.sony.input.BindCaptureTask;
import vegas.sony.ui.click.frame.impl.main.ClickGuiMacrosSettingsPanel;
import vegas.sony.unmap.Bendable;

class ClickGuiMacrosPrimaryBindCaptureTask
extends BindCaptureTask {
    final ClickGuiMacrosSettingsPanel settingsPanel;

    private void completeCapture() {
        this.settingsPanel.setBindCaptureTask(null);
        SonyVegas.INSTANCE.saveAndStop();
    }

    @Override
    public void onCaptureComplete() {
        this.completeCapture();
    }

    ClickGuiMacrosPrimaryBindCaptureTask(ClickGuiMacrosSettingsPanel clickGuiMacrosSettingsPanel, Bendable bendable) {
        super(bendable);
        this.settingsPanel = clickGuiMacrosSettingsPanel;
    }
}
