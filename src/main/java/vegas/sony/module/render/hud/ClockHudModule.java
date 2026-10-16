package vegas.sony.module.render.hud;

import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.ClockHudFrame;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;

public class ClockHudModule
extends HudModule {
    private final ModeOption digitalMode;
    public final ModeValue clockType;
    public final BooleanValue showDate;
    private final ModeOption analogMode;
    public final BooleanValue use24HourTime = BooleanValue.create(this, "24 Hour Time //$ui", false);

    public ClockHudModule() {
        super("Clock //$ui", HudModuleGroup.HUD, "clock_mod //$", ClockHudFrame.class);
        this.showDate = BooleanValue.create(this, "Show date //$ui", true);
        this.analogMode = new ModeOption("Analog //$ui");
        this.digitalMode = new ModeOption("Digital //$ui");
        this.clockType = ModeValue.create((Object)this, "Clock Type //$ui", this.analogMode, this.analogMode, this.digitalMode);
        this.setSuffix("Draws a clock with the current real-world time //$ui");
        this.clockType.addModeDependentValues(this.digitalMode, this.showDate);
        this.addValue(this.clockType, this.showDate, this.use24HourTime);
    }
}

