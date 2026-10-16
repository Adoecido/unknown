package vegas.sony.ui.click;

import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

public interface GuiMouseListener {
    default public void g(Point point, MouseClickButton uA) {
    }

    default public boolean Q(Point point) {
        return false;
    }

    default public void I(Point point) {
    }
}

