package vegas.sony.module.none;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;

public class MouseDelayFix
extends HudModule {
    public MouseDelayFix() {
        super("MouseDelayFix //$ui", HudModuleGroup.GAME, "mouse_delay_fix //$");
        this.setSuffix("Fixes the bug on version 1.8.x that causes aiming/hit registration to be less accurate.\nThe fix makes combat more similar to 1.7 //$ui");
    }
}
