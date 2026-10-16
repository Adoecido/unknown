package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.SessionSpoofFrame;

public class SessionSpoofApplyClickHandler
implements GuiClickListener {
    final SessionSpoofFrame P;


    @Override
    public void onPrimaryClick() {
        if (SessionSpoofFrame.t(this.P) != null) {
            SessionSpoofFrame.M(this.P);
        }
    }

    public SessionSpoofApplyClickHandler(SessionSpoofFrame sw_02) {
        this.P = sw_02;
    }
}

