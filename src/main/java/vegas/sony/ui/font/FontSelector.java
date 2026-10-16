package vegas.sony.ui.font;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.font.BaseFontOption;
import vegas.sony.ui.font.FontOption;
import vegas.sony.ui.font.FontOptionVariantA;
import vegas.sony.ui.font.FontOptionVariantB;
import vegas.sony.ui.font.FontOptionVariantC;
import vegas.sony.ui.font.IdentityFontOption;
import vegas.sony.ui.font.NotoFontOption;

public class FontSelector {
    private FontOption r = j;
    public static final FontOption S;
    public static final FontOption a;
    public static final FontOption P;
    public static final FontOption c;
    public static final BaseFontOption j;

    public FontOption W() {
        return this.r;
    }

    public void N(FontOption fontOption) {
        this.r = fontOption;
        ClientSettings.INSTANCE.requestFrameLayoutRefresh();
    }

    static {
        String[] stringArray = new String[]{"French", "English", "Portuguese", "Spanish", "Chinese"};
        j = new IdentityFontOption(stringArray[1]);
        S = new FontOptionVariantC(stringArray[3]);
        c = new NotoFontOption(stringArray[4]);
        a = new FontOptionVariantA(stringArray[2]);
        P = new FontOptionVariantB(stringArray[0]);
    }

    public FontSelector() {
        j.g(j);
        S.g(j);
        c.g(j);
        a.g(j);
        P.g(j);
    }
}

