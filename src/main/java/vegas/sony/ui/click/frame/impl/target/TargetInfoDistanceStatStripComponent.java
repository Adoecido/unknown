package vegas.sony.ui.click.frame.impl.target;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.frame.impl.target.TargetInfoStatStripComponent;
import vegas.sony.ui.font.SmoothFontRenderer;
import java.awt.Color;

public class TargetInfoDistanceStatStripComponent
extends TargetInfoStatStripComponent {
    private int comparisonValue;

    public TargetInfoDistanceStatStripComponent() {
        super(16, 10);
    }

    public void setComparisonValue(int comparisonValue) {
        this.comparisonValue = comparisonValue;
    }


    @Override
    public void H() {
        String string;
        super.H();
        Color color = new Color(255, 255, 255, 180);
        String string2 = string = this.comparisonValue >= 0 ? "+" + String.valueOf(this.comparisonValue) : String.valueOf(this.comparisonValue);
        if (this.comparisonValue >= 9) {
            string = "9+";
        } else if (this.comparisonValue <= -9) {
            string = "-9";
        }
        Color color2 = this.frame != null ? this.frame.applyDefaultEditorAlpha(color) : color;
        SmoothFontRenderer smoothFontRenderer = SonyVegas.INSTANCE.getFontManager().K(0.9f, true);
        smoothFontRenderer.d(string, (double)((float)this.G$src$D$1b2f02a()) + this.A() - smoothFontRenderer.N(string) - 5.0, (float)this.n() + 1.0f, color2);
    }
}

