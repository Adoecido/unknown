package vegas.sony.module.none;

import vegas.sony.SonyVegas;
import vegas.sony.module.MinecraftVersionConstraint;
import vegas.sony.module.Mod;
import vegas.sony.module.none.textgui.TextGuiModuleWidthComparator;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.NameComparator;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.ModuleNameSuggestionProvider;
import vegas.sony.value.NumberValue;
import vegas.sony.value.OptionalLimitValue;
import vegas.sony.value.StringValue;
import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

public class TextGuiSettings
extends ConfigSettingsModule {
    public final BooleanValue smoothFont;
    public final ModeOption basicSuffixMode;
    public final BooleanValue rescale;
    public final ModeValue colorMode;
    public final ModeValue suffixMode;
    public final ModeOption sortByLength;
    public final ModeValue sortMode;
    public final BooleanValue addCustomText;
    public final ModeOption matchGuiColor;
    public final ModeOption extendedSuffixMode;
    public final NumberValue scale;
    public final BooleanValue clickDisable;
    public final ModeOption moduleColor;
    public final ColorValue textGuiColor;
    public final StringValue customText;
    public final ModeOption customColor;
    public final BooleanValue shadow;
    public final BooleanValue hideModules;
    public final ModeOption noSuffix;
    public static TextGuiSettings INSTANCE;
    public final OptionalLimitValue hiddenModules;
    public final BooleanValue customTextColorEnabled;
    public final ModeOption alphabeticalSort = new ModeOption("Alphabetical //$ui");
    public final BooleanValue watermark;
    public final ColorValue customTextColor;
    public final BooleanValue renderBackground;
    public final BooleanValue animations;

    public int getSuffixModeIndex() {
        return this.suffixMode.getValue() == this.basicSuffixMode ? 0 : (this.suffixMode.getValue() == this.extendedSuffixMode ? 1 : 2);
    }

    public String getEnabledModuleNames() {
        CopyOnWriteArrayList<Mod> copyOnWriteArrayList = new CopyOnWriteArrayList<Mod>(SonyVegas.INSTANCE.getModManager().collectMods());
        if (this.sortMode.getValue() == this.alphabeticalSort) {
            copyOnWriteArrayList.sort(new NameComparator());
        } else if (this.sortMode.getValue() == this.sortByLength) {
            copyOnWriteArrayList.sort(new TextGuiModuleWidthComparator());
        }
        String string = "  ";
        for (Mod mod : copyOnWriteArrayList) {
            if (!mod.isEnabled() || mod.getGuiColor() == 0) continue;
            string = string + mod.getName() + ", ";
        }
        if (string.length() > 2) {
            string = string.substring(0, string.length() - 2);
        }
        return string;
    }

    public TextGuiSettings() {
        super("Text GUI //$ui");
        this.sortByLength = new ModeOption("Length //$ui");
        this.sortMode = ModeValue.create((Object)this, "Sort //$ui", this.sortByLength, this.sortByLength, this.alphabeticalSort);
        this.moduleColor = new ModeOption("Module color //$ui");
        this.matchGuiColor = new ModeOption("Match GUI color //$ui");
        this.customColor = new ModeOption("Custom color //$ui");
        this.colorMode = ModeValue.create((Object)this, "Color Mode //$ui", this.moduleColor, this.moduleColor, this.matchGuiColor, this.customColor);
        this.textGuiColor = ColorValue.create(this, "Text GUI color //$ui", new Color(206, 7, 7));
        this.clickDisable = BooleanValue.create(this, "Click disable //$ui", false, "Click modules in text gui to toggle them //$ui");
        this.shadow = BooleanValue.create(this, "Shadow //$ui", true, "Renders shadowed text //$ui");
        this.animations = BooleanValue.create(this, "Animations //$ui", true, "Use animations on text gui //$ui");
        this.watermark = BooleanValue.create(this, "Watermark //$ui", false, "Renders a watermark //$ui");
        this.renderBackground = BooleanValue.create(this, "Render background //$ui", true);
        this.hideModules = BooleanValue.create(this, "Hide modules //$ui", false, "Allows you to blacklist certain modules from being shown //$ui");
        this.hiddenModules = (OptionalLimitValue)OptionalLimitValue.createWithDescription(this, "module-show-blacklist //$ui", "Hidden Modules //$ui", "Name of module to hide //$ui", OptionalLimitValue.BLOCK_LIST_COLOR, Arrays.asList("ESP //$", "NameTags //$", "StorageESP //$")).setSuggestionProvider(new ModuleNameSuggestionProvider());
        this.rescale = BooleanValue.create(this, "Rescale //$ui", true, "Rescales text GUI //$ui");
        this.scale = NumberValue.create((Object)this, "Scale //$ui", "#.#", "", 0.1, 1.0, 2.0, 0.1);
        this.addCustomText = BooleanValue.create(this, "Add custom text //$ui", false);
        this.customText = StringValue.create(this, "Custom text //$ui", "");
        this.customTextColorEnabled = BooleanValue.create(this, "Set custom text color //$ui", false);
        this.customTextColor = ColorValue.create(this, "Color of custom text //$ui", new Color(206, 7, 7));
        this.smoothFont = BooleanValue.create(this, "Smooth font //$ui", true);
        this.basicSuffixMode = new ModeOption("Basic //$ui");
        this.extendedSuffixMode = new ModeOption("Extended //$ui");
        this.noSuffix = new ModeOption("None //$ui");
        this.suffixMode = ModeValue.create((Object)this, "Suffix mode //$ui", this.basicSuffixMode, this.basicSuffixMode, this.extendedSuffixMode, this.noSuffix);
        INSTANCE = this;
        this.addValue(this.sortMode, this.colorMode, this.textGuiColor, this.suffixMode, this.clickDisable, this.shadow, this.animations, this.watermark, this.renderBackground, this.hideModules, this.hiddenModules, this.rescale, this.scale, this.addCustomText, this.customText, this.customTextColorEnabled, this.customTextColor);
        this.U(this.smoothFont, new MinecraftVersionConstraint[0]);
        this.customText.setBase64Encoded(true);
        this.colorMode.addActiveMode(this.textGuiColor, this.customColor);
        this.hideModules.addDependentValues(this.hiddenModules);
        this.addCustomText.addDependentValues(this.customText);
        this.addCustomText.addDependentValues(this.customTextColorEnabled);
        this.customTextColorEnabled.addDependentValues(this.customTextColor);
    }

}

