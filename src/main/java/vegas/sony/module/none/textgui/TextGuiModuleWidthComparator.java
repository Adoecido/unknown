package vegas.sony.module.none.textgui;

import vegas.sony.SonyVegas;
import vegas.sony.module.Mod;
import vegas.sony.module.none.TextGuiSettings;
import vegas.sony.ui.font.SmoothFontRenderer;
import java.util.Comparator;

public class TextGuiModuleWidthComparator
implements Comparator<Mod> {
    @Override
    public int compare(Mod mod, Mod mod2) {
        SmoothFontRenderer smoothFontRenderer = SonyVegas.INSTANCE.getFontManager().Y();
        return Double.compare(smoothFontRenderer.N(mod2.getName() + mod2.getSuffixForMode(TextGuiSettings.INSTANCE.getSuffixModeIndex())), smoothFontRenderer.N(mod.getName() + mod.getSuffixForMode(TextGuiSettings.INSTANCE.getSuffixModeIndex())));
    }

    public TextGuiModuleWidthComparator() {
    }
}
