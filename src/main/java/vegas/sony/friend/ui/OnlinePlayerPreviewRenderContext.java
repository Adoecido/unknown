package vegas.sony.friend.ui;

import vegas.sony.render.OffscreenRenderContext;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public class OnlinePlayerPreviewRenderContext
extends OffscreenRenderContext {
    private boolean levelView;

    public OnlinePlayerPreviewRenderContext() {
        super(true);
    }

    @Override
    public void renderOffscreenFrame() {
        EntityPlayerSP entityPlayerSP = Minecraft.thePlayer();
        this.captureInterpolatedCameraPosition(entityPlayerSP);
        this.cameraYaw = entityPlayerSP.J() + 180.0f;
        this.cameraPitch = this.levelView ? 0.0f : -entityPlayerSP.V();
        super.renderOffscreenFrame();
    }


    public void setLevelView(boolean levelView) {
        this.levelView = levelView;
    }
}

