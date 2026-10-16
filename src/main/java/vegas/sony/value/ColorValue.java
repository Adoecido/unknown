package vegas.sony.value;

import com.google.gson.JsonObject;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.unmap.ColorUtil;
import vegas.sony.utils.MutableColor;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.Value;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class ColorValue
extends Value<Object[], ColorValue> {
    private final NumberValue alphaValue;
    private final NumberValue hueValue;
    private boolean colorTransformEnabled = false;
    private final NumberValue brightnessValue;
    private final BooleanValue rainbowValue;
    private final NumberValue saturationValue;
    private boolean fadeEnabled;
    private boolean fadeSupported = true;
    private boolean rainbowSupported = true;
    private boolean alphaSupported = true;
    private Color fadeColor;
    public static final int MAX_CHANNEL_VALUE;
    private final List<Value<?, ?>> componentValues;

    public float getBrightnessRatio() {
        return ((Double)this.brightnessValue.getValue()).floatValue() / (float)this.brightnessValue.getMaximum();
    }

    @Override
    public void reset() {
        super.reset();
        if (this.isResettable()) {
            this.getHueValue().reset();
            this.getSaturationValue().reset();
            this.getBrightnessValue().reset();
            this.getAlphaValue().reset();
            this.rainbowValue.reset();
            this.fadeEnabled = false;
            this.fadeColor = this.configuredColor();
        }
    }

    @Override
    public void parse(String serializedValue) {
    }

    private void clampChannelValues() {
        this.clampChannelValue(this.hueValue);
        this.clampChannelValue(this.brightnessValue);
        this.clampChannelValue(this.saturationValue);
        this.clampChannelValue(this.alphaValue);
    }

    public void setComponentValues(Object[] components) {
        if (components.length < this.componentValues.size()) {
            return;
        }
        double hue = (Double)components[0];
        double brightness = (Double)components[1];
        double saturation = (Double)components[2];
        double alpha = (Double)components[3];
        if (hue > this.getHueValue().getMaximum()) {
            hue = (Double)this.getHueValue().getValue();
        }
        if (brightness > this.getBrightnessValue().getMaximum()) {
            brightness = (Double)this.getBrightnessValue().getValue();
        }
        if (saturation > this.getSaturationValue().getMaximum()) {
            saturation = (Double)this.getSaturationValue().getValue();
        }
        if (alpha > this.getAlphaValue().getMaximum()) {
            alpha = (Double)this.getAlphaValue().getValue();
        }
        super.setValue(components);
        this.hueValue.setValue(hue);
        this.brightnessValue.setValue(brightness);
        this.saturationValue.setValue(saturation);
        this.alphaValue.setValue(alpha);
        this.rainbowValue.setValue(Boolean.valueOf(
                (Boolean)components[4] && this.rainbowSupported));
        this.notifyChanged();
    }

    @Override
    public boolean isDefault() {
        return super.isDefault() && this.hueValue.isDefault() && this.saturationValue.isDefault() && this.brightnessValue.isDefault() && this.alphaValue.isDefault() && this.rainbowValue.isDefault() && !this.fadeEnabled;
    }

    public MutableColor getMutableColor() {
        this.clampChannelValues();
        int rgb = Color.HSBtoRGB(this.getHueRatio(), this.getSaturationRatio(), this.getBrightnessRatio());
        if (this.fadeEnabled && this.fadeColor != null) {
            Color primary = new Color(rgb);
            float progress = (float)((Math.sin(System.currentTimeMillis() / 540.0) + 1.0) * 0.5);
            int red = Math.round(primary.getRed() + (this.fadeColor.getRed() - primary.getRed()) * progress);
            int green = Math.round(primary.getGreen() + (this.fadeColor.getGreen() - primary.getGreen()) * progress);
            int blue = Math.round(primary.getBlue() + (this.fadeColor.getBlue() - primary.getBlue()) * progress);
            rgb = red << 16 | green << 8 | blue;
        }
        MutableColor mutableColor = new MutableColor(rgb);
        mutableColor.withAlpha(((Double)this.alphaValue.getValue()).intValue());
        return mutableColor;
    }

    public NumberValue getSaturationValue() {
        return this.saturationValue;
    }

    @Override
    public Color getDisplayColor() {
        MutableColor mutableColor = this.getMutableColor();
        return new Color(mutableColor.getRed(), mutableColor.getGreen(), mutableColor.getBlue());
    }

    static {
        long packedMaxChannelValue = -6336883246319533825L;
        MAX_CHANNEL_VALUE = (int)packedMaxChannelValue;
    }

    public ColorValue copyDefinition() {
        ColorValue colorValue = new ColorValue(null, this.getName(), this.getHueRatio() * 255.0f, this.getBrightnessRatio() * 255.0f, this.getSaturationRatio() * 255.0f, this.getAlphaRatio() * 255.0f);
        colorValue.setDefaultValue(this.getDefaultValue());
        colorValue.hueValue.setRoundedDefaultValue((Double)this.hueValue.getDefaultValue());
        colorValue.brightnessValue.setRoundedDefaultValue((Double)this.brightnessValue.getDefaultValue());
        colorValue.saturationValue.setRoundedDefaultValue((Double)this.saturationValue.getDefaultValue());
        colorValue.alphaValue.setRoundedDefaultValue((Double)this.alphaValue.getDefaultValue());
        colorValue.rainbowValue.setDefaultValue(this.rainbowValue.getDefaultValue());
        colorValue.rainbowSupported = this.rainbowSupported;
        colorValue.fadeSupported = this.fadeSupported;
        colorValue.fadeEnabled = this.fadeSupported && this.fadeEnabled;
        colorValue.fadeColor = this.fadeColor == null ? null : new Color(this.fadeColor.getRGB(), true);
        return colorValue;
    }

    @Override
    public ColorValue copyValueDefinition() {
        return this.copyDefinition();
    }

    @Override
    public boolean loadJson(JsonObject jsonObject) {
        if (super.loadJson(jsonObject)) {
            if (jsonObject.has("hue")) {
                this.hueValue.setValue(jsonObject.get("hue").getAsDouble() / 96.0 * 255.0);
            }
            if (jsonObject.has("hue2")) {
                this.hueValue.setValue(jsonObject.get("hue2").getAsDouble());
            }
            if (jsonObject.has("saturation")) {
                this.saturationValue.setValue(jsonObject.get("saturation").getAsDouble() / 96.0 * 255.0);
            }
            if (jsonObject.has("saturation2")) {
                this.saturationValue.setValue(jsonObject.get("saturation2").getAsDouble());
            }
            if (jsonObject.has("brightness")) {
                this.brightnessValue.setValue(jsonObject.get("brightness").getAsDouble() / 96.0 * 255.0);
            }
            if (jsonObject.has("brightness2")) {
                this.brightnessValue.setValue(jsonObject.get("brightness2").getAsDouble());
            }
            if (jsonObject.has("alpha")) {
                this.alphaValue.setValue(jsonObject.get("alpha").getAsDouble() / 96.0 * 255.0);
            }
            if (jsonObject.has("alpha2")) {
                this.alphaValue.setValue(jsonObject.get("alpha2").getAsDouble());
            }
            if (jsonObject.has("rainbow")) {
                this.rainbowValue.setValue(
                        jsonObject.get("rainbow").getAsBoolean() && this.rainbowSupported);
            }
            this.fadeEnabled = this.fadeSupported && jsonObject.has("fade") && jsonObject.get("fade").getAsBoolean();
            if (jsonObject.has("fadeColor")) {
                try {
                    this.fadeColor = new Color(Integer.parseInt(jsonObject.get("fadeColor").getAsString(), 16) | 0xFF000000, true);
                } catch (RuntimeException ignored) {
                    this.fadeColor = this.configuredColor();
                }
            }
            return true;
        }
        return false;
    }

    public boolean matchesColor(Color color) {
        return Math.abs(color.getRed() - this.getMutableColor().getRed()) < 2 && Math.abs(color.getGreen() - this.getMutableColor().getGreen()) < 2 && Math.abs(color.getBlue() - this.getMutableColor().getBlue()) < 2 && Math.abs(color.getAlpha() - this.getMutableColor().getAlpha()) < 2;
    }

    @Override
    public String getDisplayValue() {
        return "";
    }

    public NumberValue getAlphaValue() {
        return this.alphaValue;
    }

    public Object[] getComponentValues() {
        Object[] components = new Object[this.componentValues.size()];
        for (int index = 0; index < this.componentValues.size(); ++index) {
            components[index] = this.componentValues.get(index).getValue();
        }
        return components;
    }

    public void applyConfiguredColorTransform() {
        this.setPersistenceSuppressed(true);
        if (this.colorTransformEnabled && this.isRainbowEnabled()) {
            double hue = (Double)this.hueValue.getValue();
            this.setColor(ColorUtil.createReadableHsbColor(this.getHueRatio(), this.getBrightnessRatio(), this.getSaturationRatio()));
            this.hueValue.setValue(hue);
        }
        this.setPersistenceSuppressed(false);
    }

    @Override
    public void setPersistenceSuppressed(boolean suppressed) {
        super.setPersistenceSuppressed(suppressed);
        this.hueValue.setPersistenceSuppressed(suppressed);
        this.brightnessValue.setPersistenceSuppressed(suppressed);
        this.saturationValue.setPersistenceSuppressed(suppressed);
        this.alphaValue.setPersistenceSuppressed(suppressed);
        this.rainbowValue.setPersistenceSuppressed(suppressed);
    }

    public boolean isRainbowEnabled() {
        return this.rainbowSupported && this.rainbowValue.getEffectiveValue();
    }

    /**
     * Turns the rainbow off for good on values whose renderer animates the
     * colour itself. The hue slider then stays a plain colour picker and no
     * remote panel can switch this value back to rainbow.
     */
    public void setRainbowSupported(boolean supported) {
        this.rainbowSupported = supported;
        if (!supported && this.rainbowValue.getEffectiveValue().booleanValue()) {
            this.rainbowValue.setValue(false);
        }
    }

    public boolean isRainbowSupported() {
        return this.rainbowSupported;
    }

    /**
     * Disables the secondary colour that belongs to ColorValue's optional Fade
     * effect.  Some renderers already blend two dedicated colours themselves,
     * so exposing a third picker there is confusing and has no useful effect.
     */
    public void setFadeSupported(boolean supported) {
        this.fadeSupported = supported;
        if (!supported && this.fadeEnabled) {
            this.fadeEnabled = false;
            this.fadeColor = this.configuredColor();
            this.notifyChanged();
        }
    }

    public boolean isFadeSupported() {
        return this.fadeSupported;
    }

    public void setAlphaSupported(boolean supported) {
        this.alphaSupported = supported;
    }

    public boolean isAlphaSupported() {
        return this.alphaSupported;
    }

    public void setColor(Color color) {
        if (color == null) {
            return;
        }
        // Palette clicks used to change H, S, B and alpha one at a time.  Each
        // channel publishes immediately, so the profile/remote mirror could
        // capture an intermediate colour and restore it over the final choice.
        // Treat a picker choice as one logical value change instead.
        this.setColorAndEffect(color, this.isRainbowEnabled(), this.fadeEnabled,
                this.getFadeColor());
    }

    public boolean isFadeEnabled() {
        return this.fadeEnabled;
    }

    public Color getFadeColor() {
        return this.fadeColor == null ? this.configuredColor() : new Color(this.fadeColor.getRGB(), true);
    }

    private Color configuredColor() {
        int rgb = Color.HSBtoRGB(this.getHueRatio(), this.getSaturationRatio(), this.getBrightnessRatio());
        return new Color(((Double)this.alphaValue.getValue()).intValue() << 24 | rgb & 0x00FFFFFF, true);
    }

    /**
     * Applies every color channel as one logical change. Remote controls used to
     * publish four intermediate HSB states and then the rainbow state, allowing
     * profile capture to observe a partially updated color.
     */
    public void setColorAndRainbow(Color color, boolean rainbow) {
        this.setColorAndEffect(color, rainbow, false, this.fadeColor);
    }

    public void setColorAndEffect(Color color, boolean rainbow, boolean fade, Color secondaryColor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.setPersistenceSuppressed(true);
        try {
            this.hueValue.setValue((double)hsb[0] * this.hueValue.getMaximum());
            this.saturationValue.setValue((double)hsb[1] * this.saturationValue.getMaximum());
            this.brightnessValue.setValue((double)hsb[2] * this.brightnessValue.getMaximum());
            this.alphaValue.setValue(Double.valueOf(color.getAlpha()));
            this.rainbowValue.setValue(Boolean.valueOf(rainbow && this.rainbowSupported));
            this.fadeEnabled = fade && this.fadeSupported;
            this.fadeColor = secondaryColor == null ? new Color(color.getRGB(), true)
                    : new Color(secondaryColor.getRGB(), true);
            this.syncCompositeValue();
        } finally {
            this.setPersistenceSuppressed(false);
        }
        this.notifyChanged();
    }

    public int toRgb() {
        return Color.HSBtoRGB(this.getHueRatio(), this.getSaturationRatio(), this.getBrightnessRatio());
    }

    public float getSaturationRatio() {
        return ((Double)this.saturationValue.getValue()).floatValue() / (float)this.saturationValue.getMaximum();
    }

    public Object[] getComponentValuesCompat() {
        return this.getComponentValues();
    }

    @Override
    public JsonObject toJson(boolean includeValue) {
        JsonObject jsonObject = this.toJson();
        if (!this.hueValue.isDefault()) {
            jsonObject.addProperty("hue2", (Number)this.hueValue.getValue());
        }
        if (!this.saturationValue.isDefault()) {
            jsonObject.addProperty("saturation2", (Number)this.saturationValue.getValue());
        }
        if (!this.brightnessValue.isDefault()) {
            jsonObject.addProperty("brightness2", (Number)this.brightnessValue.getValue());
        }
        if (!this.alphaValue.isDefault()) {
            jsonObject.addProperty("alpha2", (Number)this.alphaValue.getValue());
        }
        if (!this.rainbowValue.isDefault()) {
            jsonObject.addProperty("rainbow", this.rainbowValue.getEffectiveValue());
        }
        if (this.fadeEnabled) {
            jsonObject.addProperty("fade", true);
            jsonObject.addProperty("fadeColor", String.format("%06X", this.getFadeColor().getRGB() & 0xFFFFFF));
        }
        return jsonObject;
    }

    ColorValue(Object owner, String name, double hue, double brightness, double saturation, double alpha) {
        super(owner, name, new Object[0]);
        this.hueValue = NumberValue.create(this, name + " hue", name + " hue", "", 0.0, hue, 255.0);
        this.brightnessValue = NumberValue.create(this, name + " brightness", name + " brightness", "", 0.0, brightness, 255.0);
        this.saturationValue = NumberValue.create(this, name + " saturation", name + " saturation", "", 0.0, saturation, 255.0);
        this.alphaValue = NumberValue.create(this, name + " alpha", name + " alpha", "", 0.0, alpha, 255.0);
        this.rainbowValue = BooleanValue.createWithDisplayName(this, name + " rainbow", name + " rainbow", false);
        int initialRgb = Color.HSBtoRGB((float)(hue / 255.0), (float)(saturation / 255.0), (float)(brightness / 255.0));
        this.fadeColor = new Color(((int)alpha << 24) | (initialRgb & 0x00FFFFFF), true);
        this.componentValues = Arrays.asList(this.hueValue, this.brightnessValue, this.saturationValue, this.alphaValue, this.rainbowValue);
        // NumberValue already mirrors a changed HSB/alpha channel back to this
        // composite ColorValue.  Rainbow is a BooleanValue, so it needs the
        // equivalent bridge for a live picker update to reach HUD renderers.
        this.rainbowValue.addChangeListener(ignored -> this.syncCompositeValue());
        Object[] defaultComponents = new Object[this.componentValues.size()];
        for (int index = 0; index < this.componentValues.size(); ++index) {
            defaultComponents[index] = this.componentValues.get(index).getDefaultValue();
        }
        this.defaultValue = defaultComponents;
    }

    public NumberValue getHueValue() {
        return this.hueValue;
    }

    public void advanceRainbowHue() {
        this.setPersistenceSuppressed(true);
        double hue = (Double)this.getHueValue().getValue() + (Double)ClientSettings.INSTANCE.rainbowSpeed.getValue();
        if (hue > this.getHueValue().getMaximum() || hue < this.getHueValue().getMinimum()) {
            hue = this.getHueValue().getMinimum();
        }
        this.hueValue.setValue(hue);
        this.setPersistenceSuppressed(false);
    }

    public void setRainbowEnabled(boolean enabled) {
        this.rainbowValue.setValue(enabled && this.rainbowSupported);
        this.setComponentValues(this.getComponentValues());
    }

    public double getHueMaximum() {
        return this.hueValue.getMaximum();
    }

    public void setColorTransformEnabled(boolean enabled) {
        this.colorTransformEnabled = enabled;
    }

    public NumberValue getBrightnessValue() {
        return this.brightnessValue;
    }

    public static ColorValue createWithAlpha(Object owner, String name, Color color, int alpha) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float hue = hsb[0];
        float saturation = hsb[1];
        float brightness = hsb[2];
        return ColorValue.createFromChannels(owner, name, hue * 255.0f, brightness * 255.0f, saturation * 255.0f, alpha);
    }

    public float getAlphaRatio() {
        return ((Double)this.alphaValue.getValue()).floatValue() / (float)this.alphaValue.getMaximum();
    }

    public static ColorValue createFromChannels(Object owner, String name, double hue, double brightness, double saturation, double alpha) {
        return new ColorValue(owner, name, hue, brightness, saturation, alpha);
    }

    public boolean isColorTransformEnabled() {
        return this.colorTransformEnabled;
    }


    public static ColorValue create(Object owner, String name, Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float hue = hsb[0];
        float saturation = hsb[1];
        float brightness = hsb[2];
        int alpha = color.getAlpha();
        return ColorValue.createFromChannels(owner, name, hue * 255.0f, brightness * 255.0f, saturation * 255.0f, alpha);
    }

    private void clampChannelValue(NumberValue numberValue) {
        if ((Double)numberValue.getValue() < numberValue.getMinimum()) {
            numberValue.setValue(numberValue.getMinimum());
        }
        if ((Double)numberValue.getValue() > numberValue.getMaximum()) {
            numberValue.setValue(numberValue.getMaximum());
        }
    }

    public void syncCompositeValue() {
        if (this.componentValues == null) {
            return;
        }
        Object[] components = this.getComponentValues();
        Object[] previous = this.getValue();
        if (Arrays.equals(previous, components)) {
            return;
        }
        // Color editors manipulate the HSB/alpha child values directly.  The
        // old code copied those channels into the parent silently, so modules
        // listening to the ColorValue (and the profile/remote sync path) did
        // not learn about the new color until the module was toggled again.
        super.setValue(components);
        this.notifyChanged();
    }

    public double getHueMinimum() {
        return this.hueValue.getMinimum();
    }

    public Object[] getComponentValuesSnapshot() {
        return this.getComponentValues();
    }

    public float getHueRatio() {
        return ((Double)this.hueValue.getValue()).floatValue() / (float)this.hueValue.getMaximum();
    }
}
