package vegas.sony.ui.font;

import vegas.sony.ui.font.FontFamily;
import vegas.sony.ui.font.FontOption;

public class NotoFontOption
extends FontOption {
    public NotoFontOption(String string) {
        super(string);
    }

    @Override
    public FontFamily b() {
        return FontFamily.NOTO;
    }
}

