package vegas.sony.ui.click.component.input;

import vegas.sony.SonyVegas;
import vegas.sony.input.BindCaptureTask;
import vegas.sony.ui.click.component.input.BindableInputComponent;
import vegas.sony.unmap.Bendable;

public class BindableInputComponentSavingBindCaptureTask
extends BindCaptureTask {
    final BindableInputComponent owner;

    public BindableInputComponentSavingBindCaptureTask(BindableInputComponent bindableInputComponent, Bendable bendable) {
        super(bendable);
        this.owner = bindableInputComponent;
    }

    public void saveSettings() {
        SonyVegas.INSTANCE.saveAndStop();
    }

    @Override
    public void onCaptureComplete() {
        this.saveSettings();
    }
}
