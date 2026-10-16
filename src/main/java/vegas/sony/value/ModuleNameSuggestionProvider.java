package vegas.sony.value;

import vegas.sony.SonyVegas;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.value.AbstractListValueSuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.UnmodifiableView;

public class ModuleNameSuggestionProvider
extends AbstractListValueSuggestionProvider {
    private List<String> cachedModuleNames;
    private boolean excludeOtherCategory;
    private static GuiComponent[] legacyComponents;

    public ModuleNameSuggestionProvider(boolean excludeOtherCategory) {
        this.excludeOtherCategory = excludeOtherCategory;
    }

    public static GuiComponent[] getLegacyComponents() {
        return legacyComponents;
    }

    public static void setLegacyComponents(GuiComponent[] components) {
        legacyComponents = components;
    }

    public ModuleNameSuggestionProvider() {
        this(false);
    }

    @Override
    public @UnmodifiableView List<String> getValues() {
        if (this.cachedModuleNames == null) {
            this.cachedModuleNames = new ArrayList<String>();
            for (Mod module : SonyVegas.INSTANCE.getModManager().collectMods()) {
                if (module.getCategory() == Category.NONE) continue;
                this.cachedModuleNames.add(module.getName());
            }
        }
        return this.cachedModuleNames;
    }


    static {
        if (ModuleNameSuggestionProvider.getLegacyComponents() != null) {
            ModuleNameSuggestionProvider.setLegacyComponents(new GuiComponent[3]);
        }
    }
}

