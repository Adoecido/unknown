package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineModeToggleComponent;
import vegas.sony.ui.click.component.gui.TextButton;
import java.awt.Color;

public class OnlineModeToggleRightTextButton
extends TextButton {
    private final OnlineModeToggleComponent toggle;

    public OnlineModeToggleRightTextButton(OnlineModeToggleComponent onlineModeToggleComponent, String string, double d, Color color, Color color2) {
        super(string, d, color, color2);
        this.toggle = onlineModeToggleComponent;
    }

    @Override
    public void H() {
    }
}
