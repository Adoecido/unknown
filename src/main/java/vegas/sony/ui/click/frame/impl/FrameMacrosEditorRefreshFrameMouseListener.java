package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.impl.FrameMacros;
import vegas.sony.ui.click.frame.impl.FrameMacrosEditor;
import java.awt.Point;

class FrameMacrosEditorRefreshFrameMouseListener
implements GuiMouseListener {
    final FrameMacros j;
    final FrameMacrosEditor k;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        this.j.l$src$Lvegas_sony_ui_click_frame_impl_FrameMacrosEditor_$1712c36();
    }

    FrameMacrosEditorRefreshFrameMouseListener(FrameMacrosEditor frameMacrosEditor, FrameMacros frameMacros) {
        this.k = frameMacrosEditor;
        this.j = frameMacros;
    }
}

