package vegas.sony.module.render;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.frame.impl.main.ClickGuiStyle;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.ModeValue;

/** The central ClickGui module and its selectable visual style. */
public class ClickGui extends Mod {
    public final ModeOption vapeStyle;
    public final ModeOption iceStyle;
    public final ModeValue style;

    public ClickGui() {
        super("ClickGui //$ui", 1621, Category.RENDER, "Opens the central module menu and selects its visual style //$ui");
        this.vapeStyle = new ModeOption("Vape //$ui", 0.8);
        this.iceStyle = new ModeOption("Ice //$ui", 0.8);
        this.style = ModeValue.create(this, "Style //$ui", "Choose the ClickGui appearance //$ui", this.vapeStyle, this.vapeStyle, this.iceStyle);
        this.addValue(this.style);
        this.setVisibility(false);
    }

    public ClickGuiStyle getSelectedStyle() {
        return this.iceStyle.isSelected() ? ClickGuiStyle.ICE : ClickGuiStyle.VAPE;
    }

    @Override
    public String getSimpleSuffix() {
        return this.style.getDisplayValue();
    }

    @Override
    public void onEnable() {
        ClientSettings settings = ClientSettings.INSTANCE;
        if (settings != null) {
            settings.openGui();
        }
    }

    @Override
    public void onDisable() {
        ClientSettings settings = ClientSettings.INSTANCE;
        if (settings != null && !settings.inputEnabled) {
            settings.closeGui();
        }
    }
}
