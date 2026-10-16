package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.module.render.hud.CompassHudModule;
import vegas.sony.ui.click.frame.impl.hud.CompassStripComponent;
import vegas.sony.ui.click.frame.impl.hud.HudModuleConfigFrameBase;

public class CompassHudFrame
extends HudModuleConfigFrameBase {
    private static final String FRAME_NAME = "CompassFrame";

    @Override
    public void renderHudContent() {
    }

    public CompassHudFrame() {
        super(CompassHudModule.class);
        this.h(new CompassStripComponent(this), new Object[0]);
    }

    @Override
    public double L() {
        return 30.0;
    }

    @Override
    public double A() {
        return 308.0;
    }

    @Override
    public String getName() {
        return FRAME_NAME;
    }
}
