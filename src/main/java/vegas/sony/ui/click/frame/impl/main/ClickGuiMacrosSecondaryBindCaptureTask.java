package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.SonyVegas;
import vegas.sony.input.BindCaptureTask;
import vegas.sony.ui.click.frame.impl.main.ClickGuiMacrosSettingsPanel;
import vegas.sony.unmap.Bendable;

class ClickGuiMacrosSecondaryBindCaptureTask
extends BindCaptureTask {
    final ClickGuiMacrosSettingsPanel settingsPanel;

    ClickGuiMacrosSecondaryBindCaptureTask(ClickGuiMacrosSettingsPanel clickGuiMacrosSettingsPanel, Bendable bendable) {
        super(bendable);
        this.settingsPanel = clickGuiMacrosSettingsPanel;
    }


    private void completeCapture() {
        this.settingsPanel.setBindCaptureTask(null);
        if (this.settingsPanel.getMacro().hasValidBinding()) {
            this.settingsPanel.showFullSettings();
        } else {
            this.settingsPanel.updateViewVisibility();
        }
        SonyVegas.INSTANCE.saveAndStop();
    }

    @Override
    public void onCaptureComplete() {
        this.completeCapture();
    }
}
