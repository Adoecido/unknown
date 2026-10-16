package vegas.sony.ui.click.component.value;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.DropdownSelectComponent;
import vegas.sony.ui.font.FontOption;
import vegas.sony.value.ModeValue;

public final class PublicProfileModeDropdownComponent
extends DropdownSelectComponent<FontOption> {
    public PublicProfileModeDropdownComponent(ModeValue modeValue) {
        super(modeValue);
    }

    @Override
    public void onSelectionChanged() {
        SonyVegas.INSTANCE.getFontSelector().N(this.getSelectedValue());
    }
}
