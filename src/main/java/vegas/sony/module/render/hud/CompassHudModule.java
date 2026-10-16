package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.CompassHudFrame;

public class CompassHudModule
extends HudModule {
    public CompassHudModule() {
        super("Compass //$ui", HudModuleGroup.HUD, "compass_active //$", CompassHudFrame.class);
        this.setSuffix("Shows a compass indicating your direction //$ui");
    }
}
