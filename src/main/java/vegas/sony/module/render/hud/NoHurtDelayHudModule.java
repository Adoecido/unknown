package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public class NoHurtDelayHudModule
extends HudModule {
    public NoHurtDelayHudModule() {
        super("NoHurtDelay //$ui", HudModuleGroup.GAME, "no_hurt_delay //$");
        this.setSuffix("Removes the camera delay caused by taking damage //$ui");
    }

    @EventHandler
    public void onTick(EventPrePlayerTick event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        player.I(0.0f);
    }
}
