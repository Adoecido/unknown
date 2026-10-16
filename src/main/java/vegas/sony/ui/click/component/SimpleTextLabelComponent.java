package vegas.sony.ui.click.component;

import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import java.awt.Color;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public class SimpleTextLabelComponent
extends GuiComponent {
    private float offsetX;
    @Nullable
    private Supplier<String> textSupplier;
    private float offsetY;
    private Color textColor;
    private boolean bold;
    private EncString text;
    private static String[] legacyState;
    protected double fontScale;
    private int extraHeight;

    @Override
    public void F() {
    }

    public String getText() {
        return this.text == null ? null : this.text.get();
    }

    public SimpleTextLabelComponent(String text) {
        this(text, 0.75);
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
    }


    @Nullable
    public Supplier<String> getTextSupplier() {
        return this.textSupplier;
    }

    public double getTextHeight() {
        return this.getFontRenderer(this.fontScale).d(this.getText());
    }

    public void setExtraHeight(int extraHeight) {
        this.extraHeight = extraHeight;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public double getTextWidth() {
        return this.getFontRenderer(this.fontScale).N(this.getText());
    }

    @Override
    public double C() {
        return this.getFontRenderer(this.fontScale).d("A") + (double)this.extraHeight;
    }

    @Override
    public void u() {
    }

    public void setText(String text) {
        this.text = Str.enc(text);
    }

    @Override
    public double x() {
        return this.getFontRenderer(this.fontScale).N(this.getText());
    }

    public void setTextSupplier(@Nullable Supplier<String> textSupplier) {
        this.textSupplier = textSupplier;
        if (textSupplier != null) {
            this.setText(textSupplier.get());
        }
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public double getFontScale() {
        return this.fontScale;
    }

    @Override
    public void I() {
    }

    public static String[] getLegacyState() {
        return legacyState;
    }

    static {
        SimpleTextLabelComponent.setLegacyState((String[])null);
    }

    public SimpleTextLabelComponent(String text, double fontScale) {
        this.textColor = SimpleTextLabelComponent.J.h;
        this.offsetX = 5.0f;
        this.offsetY = 0.0f;
        this.extraHeight = 0;
        this.text = Str.enc(text);
        this.fontScale = fontScale;
    }

    public static void setLegacyState(String[] state) {
        legacyState = state;
    }

    public boolean isBold() {
        return this.bold;
    }

    public SimpleTextLabelComponent(String text, double fontScale, Color textColor, boolean bold) {
        this(text, fontScale, textColor);
        this.bold = bold;
    }

    @Override
    public void H() {
        Supplier<String> currentTextSupplier = this.textSupplier;
        if (currentTextSupplier != null) {
            this.setText(currentTextSupplier.get());
        }
        SmoothFontRenderer fontRenderer = this.bold ? this.getAlternateFontRenderer(this.fontScale) : this.getFontRenderer(this.fontScale);
        double textHeight = fontRenderer.d("A");
        double textY = this.n() + this.L() / 2.0 - textHeight / 2.0;
        fontRenderer.d(this.getText(), this.G$src$D$1b2f02a() + (double)this.offsetX, textY + (double)this.offsetY, this.textColor);
    }

    public Color getTextColor() {
        return this.textColor;
    }

    public void setFontScale(double fontScale) {
        this.fontScale = fontScale;
    }

    public SimpleTextLabelComponent(String text, double fontScale, Color textColor) {
        this(text, fontScale);
        this.setTextColor(textColor);
    }
}
