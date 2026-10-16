package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.FpsDisplayHudFrame;

public class FpsDisplayHudModule
extends HudModule {
    public FpsDisplayHudModule() {
        super("FPS //$ui", HudModuleGroup.HUD, "fps //$", FpsDisplayHudFrame.class);
        this.setSuffix("Shows your current frames per second //$ui");
    }
}
