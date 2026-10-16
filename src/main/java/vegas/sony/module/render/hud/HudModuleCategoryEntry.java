package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.HudModuleCategoryConfigFrame;

public class HudModuleCategoryEntry
extends HudModule {
    public HudModuleCategoryEntry() {
        super("Cooldown //$ui", HudModuleGroup.HUD, "cooldowns //$", HudModuleCategoryConfigFrame.class);
    }
}
