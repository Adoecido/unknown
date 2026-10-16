package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.ui.click.component.TextInputComponentBase;
import vegas.sony.ui.click.frame.impl.profile.ProfilePublishEditorPanel;

public class ProfilePublishSecondFixedWidthNoSubmitInputComponent
extends TextInputComponentBase {
    private final double fixedWidth;

    @Override
    public double C() {
        return 16.0;
    }

    @Override
    public void submit() {
    }

    @Override
    public double x() {
        return this.fixedWidth;
    }

    public ProfilePublishSecondFixedWidthNoSubmitInputComponent(String placeholder, double fixedWidth) {
        super(placeholder);
        this.fixedWidth = fixedWidth;
    }
}
