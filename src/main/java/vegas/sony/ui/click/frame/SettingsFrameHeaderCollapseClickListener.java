package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.CollapsibleFrame;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.SettingsFrameHeaderComponent;

class SettingsFrameHeaderCollapseClickListener
implements GuiClickListener {
    final SettingsFrameHeaderComponent C;
    final Frame V;


    @Override
    public void onPrimaryClick() {
        if (this.V instanceof CollapsibleFrame) {
            ((CollapsibleFrame)((Object)this.V)).w();
        }
    }

    SettingsFrameHeaderCollapseClickListener(SettingsFrameHeaderComponent settingsFrameHeaderComponent, Frame frame) {
        this.C = settingsFrameHeaderComponent;
        this.V = frame;
    }
}

