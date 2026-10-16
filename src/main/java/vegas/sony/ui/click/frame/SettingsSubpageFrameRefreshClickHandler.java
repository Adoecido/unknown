package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.SettingsSubpageFrame;

public class SettingsSubpageFrameRefreshClickHandler
implements GuiClickListener {
    final SettingsSubpageFrame C;

    public SettingsSubpageFrameRefreshClickHandler(SettingsSubpageFrame settingsSubpageFrame) {
        this.C = settingsSubpageFrame;
    }

    @Override
    public void onPrimaryClick() {
        this.C.w();
    }
}
