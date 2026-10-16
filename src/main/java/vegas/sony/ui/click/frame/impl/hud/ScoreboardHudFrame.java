package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.module.render.hud.ScoreboardHudModule;
import vegas.sony.ui.click.frame.impl.hud.HudModuleConfigFrameBase;
import vegas.sony.utils.Vec3d;

public class ScoreboardHudFrame
extends HudModuleConfigFrameBase {
    private final ScoreboardHudModule scoreboardModule = (ScoreboardHudModule)this.getModule();
    private static final String FRAME_NAME = "Scoreboard";

    @Override
    public String getName() {
        return FRAME_NAME;
    }

    public ScoreboardHudFrame() {
        super(ScoreboardHudModule.class);
    }

    @Override
    public void renderHudContent() {
        Vec3d vec3d = this.scoreboardModule.renderScoreboard(
                this.G$src$D$1b2f02a(), this.n(), this.shouldDrawBackground());
        this.o(vec3d.getX() + 2.0);
        this.Y(vec3d.getY());
    }

    @Override
    public boolean shouldRenderHudBackground() {
        return false;
    }
}
