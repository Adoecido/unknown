package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.module.Mod;
import vegas.sony.module.ModuleDisplayInfo;

public class ActiveModuleStackEntry {
    public final Mod module;
    public final ModuleDisplayInfo displayInfo;

    public ActiveModuleStackEntry(Mod mod, ModuleDisplayInfo moduleDisplayInfo) {
        this.module = mod;
        this.displayInfo = moduleDisplayInfo;
    }
}
