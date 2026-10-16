package vegas.sony.ui.click.frame;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.Frame;
import java.awt.Point;

public class FrameOutsideChildClickFilterMouseListener
implements GuiMouseListener {
    final Frame I;
    final GuiComponent s;
    final Frame G;

    public FrameOutsideChildClickFilterMouseListener(GuiComponent guiComponent, Frame frame, Frame frame2) {
        this.s = guiComponent;
        this.I = frame;
        this.G = frame2;
    }


    @Override
    public boolean Q(Point point) {
        if (this.I.getBounds().R(point) && !this.G.getBounds().R(point)) {
            return true;
        }
        return GuiMouseListener.super.Q(point);
    }
}

