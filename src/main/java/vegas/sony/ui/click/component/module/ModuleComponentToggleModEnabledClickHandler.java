package vegas.sony.ui.click.component.module;

import vegas.sony.module.Mod;
import vegas.sony.ui.click.component.GuiClickListener;

class ModuleComponentToggleModEnabledClickHandler
implements GuiClickListener {
    final Mod module;
    final ModuleComponent owner;


    @Override
    public void onPrimaryClick() {
        this.module.K(!this.module.isFavorite());
    }

    ModuleComponentToggleModEnabledClickHandler(ModuleComponent moduleComponent, Mod mod) {
        this.owner = moduleComponent;
        this.module = mod;
    }
}

