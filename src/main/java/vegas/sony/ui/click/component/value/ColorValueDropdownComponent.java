package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.value.ColorPaletteSliderComponent;
import vegas.sony.ui.click.component.value.ColorValueDropdownHueSliderComponent;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ColorValue;
import vegas.sony.value.ModeValue;
import java.awt.Color;

public class ColorValueDropdownComponent
extends GuiComponent {
    private final ColorPaletteSliderComponent colorSlider;
    private final ModeValue modeValue;
    private ModeSelection previousSelection;
    private final ColorValue teamColorValue;

    @Override
    public double C() {
        return 25.0;
    }

    @Override
    public void F() {
    }

    public ColorValueDropdownComponent(ModeValue modeValue) {
        this.modeValue = modeValue;
        this.teamColorValue = ColorValue.create(null, "Team Color //$ui", new Color(189, 0, 1));
        this.bindValue(modeValue);
        Color[] palette = new Color[]{new Color(189, 0, 1), new Color(253, 63, 63), new Color(215, 162, 50), new Color(254, 254, 62), new Color(0, 191, 4), new Color(64, 253, 62), new Color(65, 255, 254), new Color(0, 190, 189), new Color(1, 1, 187), new Color(61, 64, 255), new Color(254, 63, 255), new Color(190, 0, 190), new Color(255, 255, 255), new Color(190, 190, 190), new Color(63, 63, 63), new Color(17, 17, 17)};
        this.colorSlider = new ColorValueDropdownHueSliderComponent(this, "Team color", this.teamColorValue, palette);
        this.colorSlider.setDisabledOverlayColor(this.getDisabledOverlayColor());
        this.colorSlider.setSeparatedSegments(true);
        this.colorSlider.setToolTips(null);
        this.addChildren(this.colorSlider);
    }

    @Override
    public void I() {
    }

    @Override
    public void H() {
        this.onDisable();
        this.colorSlider.K(this.G$src$D$1b2f02a());
        this.colorSlider.S(this.n());
        SmoothFontRenderer smoothFontRenderer = this.getFontRenderer(0.7);
        String selectionName = ((ModeSelection)this.modeValue.getValue()).getName().substring(2);
        smoothFontRenderer.d(selectionName, this.G$src$D$1b2f02a() + this.A() - 5.0 - smoothFontRenderer.N(selectionName), this.n() + 5.0, ColorValueDropdownComponent.J.Z);
    }

    @Override
    public double x() {
        return 110.0;
    }

    private void synchronizeSliderSelection() {
        if (this.previousSelection == null) {
            this.previousSelection = (ModeSelection)this.modeValue.getValue();
            return;
        }
        ModeSelection currentSelection = (ModeSelection)this.modeValue.getValue();
        if (!this.previousSelection.equals(currentSelection)) {
            this.colorSlider.selectPaletteIndex(this.modeValue.getSelectedIndex());
        }
    }

    @Override
    public void u() {
        this.modeValue.setPersistenceSuppressed(true);
        this.colorSlider.getColorValue().setPersistenceSuppressed(true);
        this.teamColorValue.setPersistenceSuppressed(true);
        this.synchronizeSliderSelection();
        if (this.modeValue.getSelectedIndex() != this.colorSlider.getSelectedIndex()) {
            this.modeValue.setSelectedIndex(this.colorSlider.getSelectedIndex());
        }
        this.previousSelection = (ModeSelection)this.modeValue.getValue();
        this.teamColorValue.setPersistenceSuppressed(false);
        this.colorSlider.getColorValue().setPersistenceSuppressed(false);
        this.modeValue.setPersistenceSuppressed(false);
    }


    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
    }
}

