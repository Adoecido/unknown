package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.ArmorStatusHudFrame;
import vegas.sony.value.BooleanValue;

public class ArmorStatusHudModule
extends HudModule {
    public final BooleanValue compact = BooleanValue.create(this, "Compact //$ui", false);

    public ArmorStatusHudModule() {
        super("Armor Status //$ui", HudModuleGroup.HUD, "armor_status //$", ArmorStatusHudFrame.class);
        this.setSuffix("Shows your currently equipped armor, and its durability //$ui");
        this.addValue(this.compact);
    }
}
