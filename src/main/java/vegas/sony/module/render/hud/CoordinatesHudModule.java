package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.CoordinatesHudFrame;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.ModeValue;

public class CoordinatesHudModule
extends HudModule {
    public final ModeOption verticalMode = new ModeOption("Vertical //$ui");
    public final ModeOption horizontalMode = new ModeOption("Horizontal //$ui");
    public final ModeValue displayMode = ModeValue.create((Object)this, "Display Type //$ui", this.horizontalMode, this.horizontalMode, this.verticalMode);

    public CoordinatesHudModule() {
        super("Coords //$ui", HudModuleGroup.HUD, "coords //$", CoordinatesHudFrame.class);
        this.setSuffix("Shows your current XYZ coordinates //$ui");
        this.addValue(this.displayMode);
    }
}
