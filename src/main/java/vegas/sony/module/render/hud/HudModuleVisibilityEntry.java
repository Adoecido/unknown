package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;

public class HudModuleVisibilityEntry
extends HudModule {
    public HudModuleVisibilityEntry() {
        super("Toggle Sprint //$ui", HudModuleGroup.GAME, "toggle_sprint //$");
    }
}

