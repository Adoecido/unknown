package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.CloseableFrameHeaderComponent;
import vegas.sony.ui.click.frame.CollapsibleFrame;
import vegas.sony.ui.click.frame.Frame;

class FrameHeaderCloseToggleClickHandler
implements GuiClickListener {
    final Frame h;
    final CloseableFrameHeaderComponent x;

    FrameHeaderCloseToggleClickHandler(CloseableFrameHeaderComponent closeableFrameHeaderComponent, Frame frame) {
        this.x = closeableFrameHeaderComponent;
        this.h = frame;
    }

    @Override
    public void onPrimaryClick() {
        if (this.h instanceof CollapsibleFrame) {
            ((CollapsibleFrame)((Object)this.h)).w();
            CloseableFrameHeaderComponent.c(this.x, ((CollapsibleFrame)((Object)this.h)).q());
        }
    }

}

