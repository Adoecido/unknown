package vegas.sony.ui.click.component.gui;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.SimpleTextLabelComponent;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.ui.font.FormattedTextWrapping;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class WrappedTextComponent
extends SimpleTextLabelComponent {
    private double measuredWidth;
    private boolean wrappingEnabled = true;
    private boolean bold;
    private double textOffsetX = 0.0;
    private double wrapWidth;
    private List<String> wrappedLines;
    private double measuredHeight = 0.0;
    private boolean centered = false;

    @Override
    public void setBold(boolean bold) {
        this.bold = bold;
        this.wrappedLines = null;
    }

    public void setWrapWidth(double wrapWidth) {
        this.wrapWidth = wrapWidth;
        this.wrappedLines = null;
    }

    @Override
    public void setFontScale(double scale) {
        super.setFontScale(scale);
        this.wrappedLines = null;
    }

    private void renderWrappedLines() {
        if (SonyVegas.INSTANCE.getFontManager() == null) {
            return;
        }
        SmoothFontRenderer fontRenderer = this.bold ? SonyVegas.INSTANCE.getFontManager().W(this.fontScale, false) : SonyVegas.INSTANCE.getFontManager().E(this.fontScale, false);
        if (fontRenderer == null) {
            return;
        }
        double currentY = this.n();
        this.measuredWidth = 0.0;
        for (String line : this.getWrappedLines()) {
            if (this.centered) {
                fontRenderer.v(line, this.G$src$D$1b2f02a() + this.textOffsetX, currentY, this.getTextColor());
            } else {
                fontRenderer.d(line, this.G$src$D$1b2f02a() + this.textOffsetX, currentY, this.getTextColor());
            }
            currentY += fontRenderer.d(line);
            double lineWidth = fontRenderer.N(line);
            if (!(lineWidth > this.measuredWidth)) continue;
            this.measuredWidth = lineWidth;
        }
        this.measuredHeight = currentY - this.n();
        this.wrappedLines = null;
    }

    public List<String> getWrappedLines() {
        if (this.wrappedLines == null) {
            List<String> source = Arrays.asList(this.getText().split("\n", -1));
            // Fonts/render may not exist yet when an early-boot notification
            // is composed (a failed hook must degrade, never abort the boot).
            if (SonyVegas.INSTANCE.getFontManager() == null) {
                this.wrappedLines = source;
                return this.wrappedLines;
            }
            SmoothFontRenderer fontRenderer = this.bold ? SonyVegas.INSTANCE.getFontManager().W(this.fontScale, false) : SonyVegas.INSTANCE.getFontManager().E(this.fontScale, false);
            if (fontRenderer == null) {
                this.wrappedLines = source;
                return this.wrappedLines;
            }
            this.wrappedLines = this.wrappingEnabled
                    ? FormattedTextWrapping.wrap(source, this.wrapWidth, fontRenderer::N) : source;
            this.measuredWidth = 0.0;
            this.measuredHeight = 0.0;
            for (String line : this.wrappedLines) {
                this.measuredWidth = Math.max(this.measuredWidth, fontRenderer.N(line));
                this.measuredHeight += fontRenderer.d(line);
            }
        }
        return this.wrappedLines;
    }

    public void setWrappingEnabled(boolean wrappingEnabled) {
        this.wrappingEnabled = wrappingEnabled;
        this.wrappedLines = null;
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        this.wrappedLines = null;
    }

    @Override
    public void c() {
        super.c();
        this.renderWrappedLines();
    }

    public WrappedTextComponent(String text, double fontScale, Color color, boolean bold, double textOffsetX) {
        super(text, fontScale);
        this.setTextColor(color);
        this.bold = bold;
        this.textOffsetX = textOffsetX;
    }


    @Override
    public double A() {
        this.getWrappedLines();
        return this.measuredWidth;
    }

    @Override
    public double C() {
        this.getWrappedLines();
        return this.measuredHeight;
    }

    public WrappedTextComponent(String text, double fontScale, Color color, boolean bold) {
        super(text, fontScale);
        this.setTextColor(color);
        this.bold = bold;
    }

    @Override
    public void H() {
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public double getWrapWidth() {
        return this.wrapWidth;
    }

    public WrappedTextComponent(String text, double fontScale) {
        super(text, fontScale);
    }
}

