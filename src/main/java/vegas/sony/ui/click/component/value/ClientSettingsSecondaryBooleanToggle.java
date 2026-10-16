package vegas.sony.ui.click.component.value;

import vegas.sony.config.PublicProfileSettings;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import vegas.sony.value.BooleanValue;
import java.awt.Color;

public final class ClientSettingsSecondaryBooleanToggle
extends BooleanToggleComponent {
    final PublicProfileSettings publicProfileSettings;

    public ClientSettingsSecondaryBooleanToggle(BooleanValue booleanValue, PublicProfileSettings publicProfileSettings) {
        super(booleanValue);
        this.publicProfileSettings = publicProfileSettings;
    }

    @Override
    public Color getDisabledOverlayColor() {
        return ClientSettingsSecondaryBooleanToggle.J.r;
    }

    @Override
    public boolean V$src$Z$1xhop3l() {
        return this.publicProfileSettings.notifications.getEffectiveValue();
    }
}
