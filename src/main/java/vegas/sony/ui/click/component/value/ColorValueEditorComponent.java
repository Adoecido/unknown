package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.MousePosition;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.IconButtonComponent;
import vegas.sony.ui.click.component.value.ColorChannelSliderComponent;
import vegas.sony.ui.click.component.value.ColorChannelType;
import vegas.sony.ui.click.component.value.ColorPreviewSwatchComponent;
import vegas.sony.ui.click.component.value.ColorValueEditorToggleExpandedClickHandler;
import vegas.sony.ui.click.frame.FrameHeaderComponent;
import vegas.sony.utils.render.ImageRenderer;
import vegas.sony.utils.render.RenderUtils;
import vegas.sony.value.ColorValue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ColorValueEditorComponent
extends GuiComponent {
    private ColorChannelSliderComponent vibranceSlider;
    private boolean collapsed = true;
    private boolean suppressChildMouseDispatch;
    private ColorChannelSliderComponent rainbowSlider;
    private ColorChannelSliderComponent opacitySlider;
    private IconButtonComponent collapseButton = new IconButtonComponent("upcollapse", 0.3);
    private final ColorPreviewSwatchComponent previewSwatch;
    private ColorChannelSliderComponent saturationSlider;
    private static String legacyInteractionMarker;
    private ColorValue colorValue;
    private final List<ColorChannelSliderComponent> optionalSliders = new ArrayList<ColorChannelSliderComponent>();

    @Override
    public void I() {
    }

    @Override
    public void H() {
        this.onDisable();
        double x = this.G$src$D$1b2f02a();
        double y = this.n();
        double sliderX = x;
        if (this.rainbowSlider != null) {
            this.rainbowSlider.K(sliderX);
            this.rainbowSlider.S(y);
            sliderX += this.rainbowSlider.getLabelWidth() + 5.0;
        }
        this.collapseButton.K(sliderX);
        this.collapseButton.S(y + 2.5 - 2.0);
        this.collapseButton.Y(this.rainbowSlider != null ? this.rainbowSlider.L() / 2.0 : 10.0);
        this.collapseButton.o(10.0);
        this.collapseButton.setIconResource(this.collapsed ? "downexpand" : "upcollapse");
        ImageRenderer.drawImage(this.colorValue.getMutableColor(), (float)(this.G$src$D$1b2f02a() + this.A() - 5.0 - 6.0), (float)this.n() + 5.0f, "colorpreview", 6.0f, 6.0f, false);
        this.previewSwatch.K(this.G$src$D$1b2f02a() + this.A() - 10.0 - 5.0 - 6.0);
        this.previewSwatch.S(this.n() + 5.0);
        if (this.collapsed) {
            for (ColorChannelSliderComponent slider : this.optionalSliders) {
                slider.setVisible(false);
            }
        } else {
            double sliderY = y + 20.0 + 5.0;
            for (ColorChannelSliderComponent slider : this.optionalSliders) {
                slider.setVisible(true);
                slider.K(x);
                slider.S(sliderY);
                sliderY += 20.0 + 5.0;
            }
        }
    }

    @Override
    public void setUseExplicitWidth(boolean bl) {
        super.setUseExplicitWidth(bl);
        if (this.rainbowSlider != null) this.rainbowSlider.setUseExplicitWidth(bl);
        this.saturationSlider.setUseExplicitWidth(bl);
        this.vibranceSlider.setUseExplicitWidth(bl);
        if (this.opacitySlider != null) this.opacitySlider.setUseExplicitWidth(bl);
    }

    @Override
    public void F() {
        MousePosition mousePosition = RenderUtils.h();
    }

    public static String getLegacyInteractionMarker() {
        return legacyInteractionMarker;
    }

    public static void setLegacyInteractionMarker(String marker) {
        legacyInteractionMarker = marker;
    }

    public ColorValueEditorComponent(ColorValue colorValue) {
        this.colorValue = colorValue;
        this.bindValue(colorValue);
        List<GuiComponent> children = new ArrayList<GuiComponent>();

        if (colorValue.isRainbowSupported()) {
            this.rainbowSlider = new ColorChannelSliderComponent(ColorChannelType.RAINBOW, colorValue);
            this.rainbowSlider.setFallbackColorComponent(this);
            this.rainbowSlider.setDisabledOverlayColor(Color.WHITE);
            children.add(this.rainbowSlider);
        }

        this.saturationSlider = new ColorChannelSliderComponent(ColorChannelType.SATURATION, colorValue);
        this.vibranceSlider = new ColorChannelSliderComponent(ColorChannelType.VIBRANCE, colorValue);
        this.optionalSliders.add(this.saturationSlider);
        this.optionalSliders.add(this.vibranceSlider);
        this.saturationSlider.setDisabledOverlayColor(ColorValueEditorComponent.J.r);
        this.vibranceSlider.setDisabledOverlayColor(ColorValueEditorComponent.J.r);
        this.saturationSlider.setToolTips(null);
        this.vibranceSlider.setToolTips(null);
        children.add(this.saturationSlider);
        children.add(this.vibranceSlider);

        if (colorValue.isAlphaSupported()) {
            this.opacitySlider = new ColorChannelSliderComponent(ColorChannelType.OPACITY, colorValue);
            this.opacitySlider.setDisabledOverlayColor(ColorValueEditorComponent.J.r);
            this.opacitySlider.setToolTips(null);
            this.optionalSliders.add(this.opacitySlider);
            children.add(this.opacitySlider);
        }

        this.previewSwatch = new ColorPreviewSwatchComponent(colorValue);
        this.collapseButton.addClickListener(new ColorValueEditorToggleExpandedClickHandler(this));
        children.add(0, this.collapseButton);
        children.add(1, this.previewSwatch);
        this.addChildren(children.toArray(new GuiComponent[0]));
    }

    static {
        ColorValueEditorComponent.setLegacyInteractionMarker(null);
    }

    @Override
    public void u() {
    }

    @Override
    public void dispatchMouseEvent(GuiMouseEvent guiMouseEvent) {
        if (this.suppressChildMouseDispatch) {
            return;
        }
        for (GuiComponent guiComponent : this.f()) {
            if (!guiComponent.V$src$Z$1xhop3l() || !guiComponent.w$src$Z$e457mb()) continue;
            guiComponent.dispatchMouseEvent(guiMouseEvent);
            if (guiComponent instanceof FrameHeaderComponent) continue;
            return;
        }
    }

    @Override
    public void setExplicitWidth(double d) {
        super.setExplicitWidth(d);
        if (this.rainbowSlider != null) this.rainbowSlider.setExplicitWidth(d);
        this.saturationSlider.setExplicitWidth(d);
        this.vibranceSlider.setExplicitWidth(d);
        if (this.opacitySlider != null) this.opacitySlider.setExplicitWidth(d);
    }

    @Override
    public void o(double d) {
        super.o(d);
        if (this.rainbowSlider != null) this.rainbowSlider.o(d);
        this.saturationSlider.o(d);
        this.vibranceSlider.o(d);
        if (this.opacitySlider != null) this.opacitySlider.o(d);
    }

    @Override
    public double x() {
        return 110.0;
    }

    public static boolean setCollapsedCompat(ColorValueEditorComponent editor, boolean collapsed) {
        editor.collapsed = collapsed;
        return editor.collapsed;
    }

    public static boolean isCollapsedCompat(ColorValueEditorComponent editor) {
        return editor.collapsed;
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
    }

    @Override
    public double C() {
        return this.collapsed ? 25.0 : (25.0 + this.optionalSliders.size() * 25.0);
    }

}