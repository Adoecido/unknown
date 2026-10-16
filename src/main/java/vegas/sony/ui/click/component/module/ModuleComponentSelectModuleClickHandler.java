package vegas.sony.ui.click.component.module;

import vegas.sony.SonyVegas;
import vegas.sony.module.Mod;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrame;

class ModuleComponentSelectModuleClickHandler
implements GuiClickListener {
    final Mod module;
    final ModuleComponent owner;
    final ModuleCategoryFrame categoryFrame;

    ModuleComponentSelectModuleClickHandler(ModuleComponent moduleComponent, Mod mod, ModuleCategoryFrame moduleCategoryFrame) {
        this.owner = moduleComponent;
        this.module = mod;
        this.categoryFrame = moduleCategoryFrame;
    }

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getModuleProfileMetadataCodec().removeModule(this.module);
        this.categoryFrame.l$src$Z$193vdc5();
    }
}
