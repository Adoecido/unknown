package vegas.sony.ui.click.component.input;

import vegas.sony.ui.click.component.input.ModuleSearchInputComponent;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrameHeader;

public class ClientSettingsSearchInputComponent
extends ModuleSearchInputComponent {
    final ClientSettingsSearchFrameHeader ey;

    @Override
    public float getLeftInset() {
        return 2.5f;
    }

    public ClientSettingsSearchInputComponent(ClientSettingsSearchFrameHeader clientSettingsSearchFrameHeader, ClientSettingsSearchFrameHeader clientSettingsSearchFrameHeader2) {
        super(clientSettingsSearchFrameHeader2);
        this.ey = clientSettingsSearchFrameHeader;
    }
}
