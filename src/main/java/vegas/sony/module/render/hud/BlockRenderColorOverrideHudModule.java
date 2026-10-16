package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventBlockRenderColorOverride;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.utils.MutableColor;
import vegas.sony.value.ColorValue;
import java.awt.Color;

public class BlockRenderColorOverrideHudModule
extends HudModule {
    private final ColorValue colorValue = ColorValue.create(this, "Color //$ui", new Color(255, 0, 0, 127));

    @EventHandler
    public void onBlockRenderColorOverride(EventBlockRenderColorOverride event) {
        MutableColor mutableColor = this.colorValue.getMutableColor();
        EventBlockRenderColorOverride.setColor((float)((Color)mutableColor).getRed() / 255.0f, (float)((Color)mutableColor).getGreen() / 255.0f, (float)((Color)mutableColor).getBlue() / 255.0f, (float)((Color)mutableColor).getAlpha() / 255.0f);
    }

    public BlockRenderColorOverrideHudModule() {
        super("Hit Color //$ui", HudModuleGroup.GAME, "hit_color_mod //$");
        this.setSuffix("Changes the color of damaged entities //$ui");
        this.addValue(this.colorValue);
    }
}

