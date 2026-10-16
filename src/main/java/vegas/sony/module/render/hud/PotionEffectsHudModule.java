package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.PotionEffectsHudFrame;
import vegas.sony.value.BooleanValue;

public class PotionEffectsHudModule
extends HudModule {
    public final BooleanValue showPositiveEffects = BooleanValue.create(this, "Show Positive Effects //$ui", true);
    public final BooleanValue showNegativeEffects = BooleanValue.create(this, "Show Negative Effects //$ui", true);

    public PotionEffectsHudModule() {
        super("Potion Status //$ui", HudModuleGroup.HUD, "potion_status //$", PotionEffectsHudFrame.class);
        this.setSuffix("Shows your currently active potion effects //$ui");
        this.addValue(this.showPositiveEffects, this.showNegativeEffects);
    }
}
