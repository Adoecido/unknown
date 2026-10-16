package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.value.ColorChannelSliderComponent;
import vegas.sony.ui.click.component.value.ColorPaletteSliderComponent;

public class ColorPaletteRefreshClickListener
implements GuiClickListener {
    final ColorChannelSliderComponent channelSlider;
    final ColorPaletteSliderComponent paletteSlider;

    @Override
    public void onPrimaryClick() {
        this.paletteSlider.resetToMiddleColor();
    }

    public ColorPaletteRefreshClickListener(ColorChannelSliderComponent channelSlider, ColorPaletteSliderComponent paletteSlider) {
        this.channelSlider = channelSlider;
        this.paletteSlider = paletteSlider;
    }
}
