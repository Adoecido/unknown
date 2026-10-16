package vegas.sony.ui.click.frame.impl;

import vegas.sony.account.MinecraftSessionWrapper;
import vegas.sony.ui.click.component.gui.TextButton;
import vegas.sony.ui.click.frame.impl.SessionSpoofFrame;
import vegas.sony.wrapper.impl.Minecraft;
import java.awt.Color;

class SessionSpoofApplyButtonComponent
extends TextButton {
    final SessionSpoofFrame X_;
    private static final String cb = "Logged in (";

    @Override
    public boolean w$src$Z$e457mb() {
        if (SessionSpoofFrame.t(this.X_) == null) {
            return false;
        }
        return super.w$src$Z$e457mb();
    }

    @Override
    public void u() {
        MinecraftSessionWrapper minecraftSessionWrapper;
        super.u();
        if (SessionSpoofFrame.t(this.X_) == null && (minecraftSessionWrapper = Minecraft.Q$src$Lvegas_sony_account_MinecraftSessionWrapper_$1ftnn3u()).isNotNull()) {
            SessionSpoofFrame.i(this.X_).setLabelText(cb + minecraftSessionWrapper.getUsername() + ")");
        }
    }


    @Override
    public double A() {
        return 140.0;
    }

    SessionSpoofApplyButtonComponent(SessionSpoofFrame sessionSpoofFrame, String string, double d, Color color, Color color2) {
        super(string, d, color, color2);
        this.X_ = sessionSpoofFrame;
    }

    @Override
    public double L() {
        return 20.0;
    }
}
