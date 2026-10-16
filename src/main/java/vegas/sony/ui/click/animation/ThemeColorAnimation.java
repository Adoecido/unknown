package vegas.sony.ui.click.animation;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.animation.ColorAnimation;
import java.awt.Color;

public class ThemeColorAnimation
extends ColorAnimation {
    public ThemeColorAnimation(double d, Color color) {
        super(d, color, SonyVegas.INSTANCE.getClientSettings().guiColor.getMutableColor());
    }

    @Override
    public Color getInterpolatedColor() {
        super.setEndColor(SonyVegas.INSTANCE.getClientSettings().guiColor.getMutableColor());
        return super.getInterpolatedColor();
    }
}
