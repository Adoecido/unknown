package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.SettingsSubpageFrame;

public class SettingsSubpageFrameCollapseClickHandler
implements GuiClickListener {
    final SettingsSubpageFrame y;

    @Override
    public void onPrimaryClick() {
        this.y.p();
    }

    public SettingsSubpageFrameCollapseClickHandler(SettingsSubpageFrame settingsSubpageFrame) {
        this.y = settingsSubpageFrame;
    }
}
