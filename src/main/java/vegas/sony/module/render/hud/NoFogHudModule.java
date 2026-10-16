package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventFogDensity;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;

public class NoFogHudModule
extends HudModule {
    @EventHandler
    public void onFogDensity(EventFogDensity event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        if (!player.h$src$Z$ftwoya()) {
            return;
        }
        event.setCancelled(true);
        if (ForgeVersion.MC_1_16_5.d()) {
            event.setDensity(0.01f);
        }
    }


    public NoFogHudModule() {
        super("Clear Water //$ui", HudModuleGroup.GAME, "clearwater //$");
        this.setSuffix("Makes water clear when under water //$ui");
    }
}

